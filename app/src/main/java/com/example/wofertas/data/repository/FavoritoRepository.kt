package com.example.wofertas.data.repository

import android.content.Context
import com.example.wofertas.data.local.AppDatabase
import com.example.wofertas.data.local.entities.FavoritoEntity
import com.example.wofertas.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Gerencia dados de favoritos seguindo a Seção 1.2 da especificação.
 */
class FavoritoRepository(private val context: Context) {

    private val api get() = ApiClient.authService(context)
    private val db = AppDatabase.getInstance(context)
    private val dao = db.favoritoDao()
    private val mercadoDao = db.mercadoDao()
    private val analytics = AnalyticsRepository(context)

    fun getFavoritosFlow(usuarioId: String): Flow<List<FavoritoEntity>> =
        dao.getAllByUserFlow(usuarioId)

    /**
     * Lista favoritos seguindo Seção 1.2.
     */
    suspend fun fetchFavoritos(usuarioId: String): ApiResult<List<FavoritoResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.listarFavoritos(usuarioId)
                if (response.isSuccessful) {
                    val lista = response.body() ?: emptyList()
                    val mercadosPorId = carregarMercadosPorId()
                    // Sincroniza cache local (Seção 7.2)
                    dao.clearByUser(usuarioId)
                    dao.insertAll(lista.map { it.toEntity(usuarioId, mercadosPorId[it.idMercado]) })
                    ApiResult.Success(lista)
                } else {
                    val cached = dao.getAllByUser(usuarioId)
                    ApiResult.Success(cached.map { it.toDto() })
                }
            } catch (e: Exception) {
                val cached = dao.getAllByUser(usuarioId)
                if (cached.isNotEmpty()) {
                    ApiResult.Success(cached.map { it.toDto() })
                } else {
                    ApiResult.Error(e)
                }
            }
        }

    /**
     * Alterna estado de favorito (Seção 1.2).
     */
    suspend fun toggleFavorito(
        usuarioId: String,
        mercadoId: String,
        mercado: MercadoResponse? = null
    ): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        val jaFavorito = dao.isFavorito(usuarioId, mercadoId)
        try {
            val response = api.toggleFavorito(mercadoId)
            if (response.isSuccessful) {
                if (jaFavorito) {
                    dao.delete(usuarioId, mercadoId)
                } else {
                    // Analytics: Envia evento de CURTIDA ao favoritar
                    analytics.trackEvent(mercadoId, AnalyticsRepository.CURTIDA)
                    
                    dao.insert(
                        FavoritoEntity(
                            id = UUID.randomUUID().toString(),
                            usuarioId = usuarioId,
                            mercadoId = mercadoId,
                            mercadoNome = mercado?.nome ?: "",
                            mercadoImagemLogo = mercado?.logo ?: mercado?.imagemLogo,
                            mercadoEndereco = mercado?.endereco,
                            pendingSync = false
                        )
                    )
                }
                ApiResult.Success(!jaFavorito)
            } else {
                ApiResult.Error(Exception("Erro ${response.code()}"))
            }
        } catch (e: Exception) {
            // Implementação de "Otimista" ou Fallback Offline (Seção 7.3)
            if (jaFavorito) {
                dao.delete(usuarioId, mercadoId)
            } else {
                // Analytics: Envia mesmo em modo offline/erro (trackEvent já lida com falhas)
                analytics.trackEvent(mercadoId, AnalyticsRepository.CURTIDA)

                dao.insert(
                    FavoritoEntity(
                        id = UUID.randomUUID().toString(),
                        usuarioId = usuarioId,
                        mercadoId = mercadoId,
                        mercadoNome = mercado?.nome ?: "",
                        mercadoImagemLogo = mercado?.logo ?: mercado?.imagemLogo,
                        mercadoEndereco = mercado?.endereco,
                        pendingSync = true
                    )
                )
            }
            ApiResult.Success(!jaFavorito)
        }
    }

    suspend fun syncPendentes() = withContext(Dispatchers.IO) {
        val pendentes = dao.getPendingSync()
        pendentes.forEach { fav ->
            try {
                api.toggleFavorito(fav.mercadoId)
                dao.insert(fav.copy(pendingSync = false))
            } catch (e: Exception) { }
        }
    }

    private suspend fun carregarMercadosPorId(): Map<String, MercadoResponse> {
        return try {
            val response = api.listarMercados()
            if (response.isSuccessful) {
                val mercados = response.body().orEmpty()
                mercadoDao.insertAll(mercados.map { it.toEntity() })
                mercados.associateBy { it.id }
            } else {
                mercadoDao.getAll().map { it.toResponse() }.associateBy { it.id }
            }
        } catch (e: Exception) {
            mercadoDao.getAll().map { it.toResponse() }.associateBy { it.id }
        }
    }
}

// ── Mapeamentos ───────────────────────────────────────────────────────────────
fun FavoritoResponse.toEntity(usuarioId: String, mercado: MercadoResponse? = null) = FavoritoEntity(
    id = id,
    usuarioId = idUsuario.ifEmpty { usuarioId },
    mercadoId = idMercado,
    mercadoNome = mercado?.nome ?: "",
    mercadoImagemLogo = mercado?.logo ?: mercado?.imagemLogo,
    mercadoEndereco = mercado?.endereco,
    pendingSync = false
)

fun FavoritoEntity.toDto() = FavoritoResponse(
    id = id,
    idUsuario = usuarioId,
    idMercado = mercadoId
)
