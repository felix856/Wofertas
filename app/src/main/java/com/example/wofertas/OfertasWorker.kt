package com.example.wofertas

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wofertas.data.local.AppDatabase
import com.example.wofertas.data.local.entities.ProdutoEncontradoEntity
import com.example.wofertas.fcm.NotificationHelper
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.OfertaDto
import java.lang.Exception
import java.util.Locale

class OfertasWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val TAG = "WofertasWorker"
    private val db by lazy { AppDatabase.getInstance(context) }

    override suspend fun doWork(): Result {
        // 1. Verifica sessão
        if (!AuthManager.isLoggedIn(context)) {
            Log.d(TAG, "Worker abortado: Usuário não está logado.")
            return Result.success()
        }

        return try {
            Log.i(TAG, "Iniciando busca de ofertas em segundo plano...")

            val api = ApiClient.authService(context)
            val origem = LocationPrefs.getLast(context)
            val resp = if (origem != null) {
                val raioKm = LocationPrefs.getRaioMetros(context) / 1000.0
                val proximas = api.listarOfertasProximas(
                    lat = origem.latitude,
                    lng = origem.longitude,
                    raioKm = raioKm,
                    ativo = true
                )
                if (proximas.isSuccessful) proximas else api.listarOfertas(ativo = true)
            } else {
                api.listarOfertas(ativo = true)
            }

            if (resp.isSuccessful) {
                val dtos = resp.body() ?: emptyList()
                Log.d(TAG, "API retornou ${dtos.size} ofertas.")

                if (dtos.isNotEmpty()) {
                    val ofertasNoRaio = filtrarOfertasNoRaio(dtos)
                    verificarProdutosDaLista(ofertasNoRaio)

                    // 2. Filtra IDs para não notificar o que o usuário já viu
                    val novosIds = filtrarApenasNovos(ofertasNoRaio.map { it.id })

                    if (novosIds.isNotEmpty()) {
                        val nomes = ofertasNoRaio.filter { novosIds.contains(it.id) }
                            .map { "${it.mercado?.nome ?: "Mercado"} — ${it.nome}" }

                        Log.i(TAG, "Notificando ${novosIds.size} novas ofertas.")
                        NotificationHelper.notificarNovasOfertas(context, novosIds, nomes)

                        // 3. Salva os IDs notificados
                        salvarIdsNotificados(novosIds)
                    } else {
                        Log.d(TAG, "Nenhuma oferta nova desde a última verificação.")
                    }
                }
                Result.success()
            } else {
                Log.e(TAG, "Erro na API: Código ${resp.code()} - ${resp.errorBody()?.string()}")
                // Se for erro de autenticação (401), não adianta tentar de novo agora
                if (resp.code() == 401) Result.failure() else Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha crítica/rede no Worker: ${e.message}", e)
            Result.retry()
        }
    }

    // --- Lógica Auxiliar para evitar Spam ---

    private fun filtrarApenasNovos(idsAtuais: List<String>): List<String> {
        val prefs = context.getSharedPreferences("worker_prefs", Context.MODE_PRIVATE)
        val jaNotificados = prefs.getStringSet("ids_notificados", emptySet()) ?: emptySet()
        return idsAtuais.filter { it !in jaNotificados }
    }

    private fun salvarIdsNotificados(novosIds: List<String>) {
        val prefs = context.getSharedPreferences("worker_prefs", Context.MODE_PRIVATE)
        // Busca os IDs antigos e transforma em um MutableSet real
        val jaNotificados = prefs.getStringSet("ids_notificados", emptySet())?.toMutableSet() ?: mutableSetOf() // <-- CORREÇÃO AQUI

        jaNotificados.addAll(novosIds)
        prefs.edit().putStringSet("ids_notificados", jaNotificados).apply()
    }

    private suspend fun verificarProdutosDaLista(ofertas: List<OfertaDto>) {
        val ofertasNoRaio = filtrarOfertasNoRaio(ofertas)
        val produtosLista = db.produtoListaDao().getAll()
        if (produtosLista.isEmpty() || ofertasNoRaio.isEmpty()) return

        val encontrados = mutableListOf<ProdutoEncontradoEntity>()

        ofertasNoRaio.forEach { oferta ->
            produtosLista.forEach { produto ->
                val precoOk = produto.precoMaximo == null || oferta.preco == null || oferta.preco <= produto.precoMaximo
                if (precoOk && OcrHelper.corresponde(produto.nome, oferta.nome)) {
                    encontrados.add(
                        ProdutoEncontradoEntity(
                            nomeProduto = produto.nome,
                            nomeEncontrado = oferta.nome,
                            preco = oferta.preco,
                            nomeMercado = oferta.mercado?.nome ?: "Mercado",
                            ofertaId = oferta.id
                        )
                    )
                }
            }
        }

        if (encontrados.isEmpty()) return

        db.produtoEncontradoDao().insertAll(encontrados)
        notificarProdutosEncontrados(encontrados)
    }

    private fun filtrarOfertasNoRaio(ofertas: List<OfertaDto>): List<OfertaDto> {
        val origem = LocationPrefs.getLast(context) ?: return ofertas
        val raio = LocationPrefs.getRaioMetros(context)

        return ofertas.filter { oferta ->
            val lat = oferta.mercado?.latitude
            val lon = oferta.mercado?.longitude
            if (lat == null || lon == null) return@filter false

            val destino = android.location.Location("").apply {
                latitude = lat
                longitude = lon
            }
            origem.distanceTo(destino) <= raio
        }
    }

    private suspend fun notificarProdutosEncontrados(encontrados: List<ProdutoEncontradoEntity>) {
        val prefs = context.getSharedPreferences("lista_compras_notificacoes", Context.MODE_PRIVATE)
        val vistos = prefs.getStringSet("keys", emptySet())?.toMutableSet() ?: mutableSetOf()
        var mudou = false

        encontrados.forEach { encontrado ->
            val key = "oferta:${encontrado.ofertaId}:${encontrado.nomeProduto}"
            if (key !in vistos) {
                vistos.add(key)
                mudou = true
                
                // Obter histórico no banco para identificar recorrência
                val historico = db.produtoEncontradoDao().getByNome(encontrado.nomeProduto)
                val recorrente = historico.size > 1
                
                val preco = encontrado.preco?.let { " · R$ %.2f".format(Locale("pt", "BR"), it) } ?: ""
                NotificationHelper.notificarProdutoEncontrado(
                    context,
                    encontrado.nomeProduto,
                    encontrado.nomeMercado,
                    preco,
                    key.hashCode(),
                    recorrente
                )
            }
        }

        if (mudou) {
            prefs.edit().putStringSet("keys", vistos).apply()
        }
    }


    companion object {
        const val WORK_NAME = "OfertasWorker"
    }
}
