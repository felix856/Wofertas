package com.example.wofertas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wofertas.data.local.AppDatabase
import com.example.wofertas.data.local.entities.ProdutoEncontradoEntity
import com.example.wofertas.data.local.entities.ProdutoListaEntity
import com.example.wofertas.network.ApiClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

class CarrinhoActivity : BaseClienteActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvVazio: TextView
    private lateinit var etNovoProduto: EditText
    private lateinit var btnAdicionar: ImageButton
    private lateinit var tvResumoItens: TextView
    private lateinit var tvResumoEncontrados: TextView
    private lateinit var tvResumoMelhorPreco: TextView
    private lateinit var tvResumoDica: TextView
    private lateinit var adapter: CarrinhoAdapter

    private val db by lazy { AppDatabase.getInstance(this) }
    private val api get() = ApiClient.authService(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrinho)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_carrinho)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerCarrinho)
        tvVazio = findViewById(R.id.tvCarrinhoVazio)
        etNovoProduto = findViewById(R.id.etNovoProduto)
        btnAdicionar = findViewById(R.id.btnAdicionarProduto)
        tvResumoItens = findViewById(R.id.tvResumoItens)
        tvResumoEncontrados = findViewById(R.id.tvResumoEncontrados)
        tvResumoMelhorPreco = findViewById(R.id.tvResumoMelhorPreco)
        tvResumoDica = findViewById(R.id.tvResumoDica)

        adapter = CarrinhoAdapter(
            onDelete = { item -> confirmarExclusao(item) },
            onSetPreco = { item -> dialogPrecoMaximo(item) },
            onOpenOferta = { encontrado -> abrirOfertaEncontrada(encontrado) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAdicionar.setOnClickListener { adicionarProduto() }
        etNovoProduto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                adicionarProduto()
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            db.produtoListaDao().getAllFlow().collectLatest { lista ->
                val encontrados = db.produtoEncontradoDao().getAll()
                tvVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
                atualizarResumo(lista, encontrados)
                adapter.atualizar(lista, encontrados)
            }
        }
    }

    private fun adicionarProduto() {
        val nome = etNovoProduto.text.toString().trim()
        if (nome.isBlank()) {
            etNovoProduto.error = "Digite o produto"
            return
        }

        lifecycleScope.launch {
            val jaExiste = db.produtoListaDao().getAll()
                .any { normalizarProduto(it.nome) == normalizarProduto(nome) }
            if (jaExiste) {
                Toast.makeText(this@CarrinhoActivity, "Produto ja esta na lista.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            db.produtoListaDao().insert(ProdutoListaEntity(nome = nome))
            etNovoProduto.text.clear()
        }
    }

    private fun confirmarExclusao(item: ProdutoListaEntity) {
        AlertDialog.Builder(this)
            .setTitle("Remover produto")
            .setMessage("Remover \"${item.nome}\" da lista?")
            .setPositiveButton("Remover") { _, _ ->
                lifecycleScope.launch { db.produtoListaDao().deleteById(item.id) }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogPrecoMaximo(item: ProdutoListaEntity) {
        val et = EditText(this).apply {
            hint = "Ex: 15.90 (vazio para qualquer preco)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item.precoMaximo?.toString() ?: "")
            setPadding(40, 20, 40, 20)
        }
        AlertDialog.Builder(this)
            .setTitle("Preco maximo para \"${item.nome}\"")
            .setView(et)
            .setPositiveButton("Salvar") { _, _ ->
                val preco = et.text.toString().replace(",", ".").toDoubleOrNull()
                lifecycleScope.launch {
                    db.produtoListaDao().updatePrecoMaximo(item.id, preco)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarResumo(
        lista: List<ProdutoListaEntity>,
        encontrados: List<ProdutoEncontradoEntity>
    ) {
        val matchesPorItem = lista.map { item -> matchesPara(item, encontrados) }
        val itensComOferta = matchesPorItem.count { it.isNotEmpty() }
        val melhorPreco = matchesPorItem.flatten()
            .mapNotNull { it.preco }
            .minOrNull()

        tvResumoItens.text = lista.size.toString()
        tvResumoEncontrados.text = itensComOferta.toString()
        tvResumoMelhorPreco.text = melhorPreco?.let { formatarPreco(it) } ?: "--"
        tvResumoDica.text = when {
            lista.isEmpty() -> "Adicione produtos para receber alertas quando uma promocao aparecer."
            itensComOferta == 0 -> "Nenhum produto da lista foi encontrado em oferta ainda. O Wofertas continua monitorando."
            itensComOferta == lista.size -> "Todos os itens da sua lista ja tem oferta encontrada. Confira os melhores mercados."
            else -> "$itensComOferta de ${lista.size} itens ja apareceram em ofertas recentes."
        }
    }

    private fun abrirOfertaEncontrada(encontrado: ProdutoEncontradoEntity) {
        if (encontrado.ofertaId.isBlank()) {
            Toast.makeText(this, "Oferta nao identificada.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = api.buscarOfertaPorId(encontrado.ofertaId)
                if (!response.isSuccessful) {
                    Toast.makeText(this@CarrinhoActivity, "Nao foi possivel abrir a oferta.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val oferta = response.body()
                val fonte = oferta?.imagemOferta ?: oferta?.imagem
                if (fonte.isNullOrBlank()) {
                    Toast.makeText(this@CarrinhoActivity, "Oferta sem encarte/imagem.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                if (fonte.contains("base64,")) {
                    DataHolder.bigString = fonte
                }

                startActivity(Intent(this@CarrinhoActivity, VerPDF::class.java).apply {
                    if (!fonte.contains("base64,")) {
                        putExtra("pdfUrl", normalizarFonte(fonte))
                    }
                    putExtra("oferta_id", oferta?.id ?: encontrado.ofertaId)
                    putExtra("oferta_nome", oferta?.nome ?: encontrado.nomeEncontrado)
                    putExtra("mercado_nome", oferta?.mercado?.nome ?: encontrado.nomeMercado)
                })
            } catch (e: Exception) {
                Toast.makeText(this@CarrinhoActivity, "Falha de conexao ao abrir oferta.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun normalizarFonte(fonte: String): String {
        if (fonte.startsWith("http", ignoreCase = true)) return fonte
        val baseUrl = ApiClient.getCurrentBaseUrl().removeSuffix("/")
        val path = if (fonte.startsWith("/")) fonte else "/$fonte"
        return baseUrl + path
    }

    private fun normalizarProduto(valor: String): String {
        val semAcento = Normalizer.normalize(valor.trim().lowercase(Locale.getDefault()), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return semAcento.replace("\\s+".toRegex(), " ")
    }

    companion object {
        fun matchesPara(
            item: ProdutoListaEntity,
            encontrados: List<ProdutoEncontradoEntity>
        ): List<ProdutoEncontradoEntity> {
            return encontrados
                .filter { enc ->
                    OcrHelper.corresponde(item.nome, enc.nomeProduto) &&
                            (item.precoMaximo == null || enc.preco == null || enc.preco <= item.precoMaximo)
                }
                .distinctBy { "${it.ofertaId}:${it.nomeMercado}:${it.preco}" }
                .sortedWith(compareBy<ProdutoEncontradoEntity> { it.preco ?: Double.MAX_VALUE }
                    .thenByDescending { it.encontradoEm })
        }

        fun formatarPreco(valor: Double): String =
            "R$ %.2f".format(Locale("pt", "BR"), valor)
    }
}

class CarrinhoAdapter(
    private val onDelete: (ProdutoListaEntity) -> Unit,
    private val onSetPreco: (ProdutoListaEntity) -> Unit,
    private val onOpenOferta: (ProdutoEncontradoEntity) -> Unit
) : RecyclerView.Adapter<CarrinhoAdapter.VH>() {

    private val items = mutableListOf<ProdutoListaEntity>()
    private val encontrados = mutableListOf<ProdutoEncontradoEntity>()

    fun atualizar(lista: List<ProdutoListaEntity>, enc: List<ProdutoEncontradoEntity>) {
        items.clear()
        items.addAll(lista)
        encontrados.clear()
        encontrados.addAll(enc)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_carrinho, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val matches = CarrinhoActivity.matchesPara(item, encontrados)
        val melhor = matches.firstOrNull()

        holder.tvNome.text = item.nome
        holder.tvPrecoMax.text = item.precoMaximo
            ?.let { "Preco maximo: ${CarrinhoActivity.formatarPreco(it)}" }
            ?: ""
        holder.tvPrecoMax.visibility = if (item.precoMaximo != null) View.VISIBLE else View.GONE

        if (melhor != null) {
            holder.tvStatusComparacao.text = "Melhor oferta encontrada"
            holder.tvStatusComparacao.setTextColor(holder.itemView.context.getColor(R.color.status_ativo))
            holder.tvStatusComparacao.setBackgroundResource(R.drawable.bg_cart_match)

            val precoStr = melhor.preco?.let { CarrinhoActivity.formatarPreco(it) } ?: "Preco a confirmar"
            holder.tvEncontrado.text = "${melhor.nomeMercado} - $precoStr"
            holder.tvEncontrado.visibility = View.VISIBLE

            val outros = matches.drop(1).take(2)
            if (outros.isNotEmpty()) {
                val detalhes = outros.joinToString("  |  ") { match ->
                    val preco = match.preco?.let { CarrinhoActivity.formatarPreco(it) } ?: "preco a confirmar"
                    "${match.nomeMercado}: $preco"
                }
                val restante = matches.size - 1 - outros.size
                holder.tvOutrosMercados.text = if (restante > 0) {
                    "$detalhes  |  +$restante oferta(s)"
                } else {
                    detalhes
                }
                holder.tvOutrosMercados.visibility = View.VISIBLE
            } else {
                holder.tvOutrosMercados.visibility = View.GONE
            }

            holder.btnVerOferta.visibility = View.VISIBLE
            holder.btnVerOferta.setOnClickListener { onOpenOferta(melhor) }
        } else {
            holder.tvStatusComparacao.text = "Aguardando oferta compativel"
            holder.tvStatusComparacao.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
            holder.tvStatusComparacao.setBackgroundResource(R.drawable.bg_field)
            holder.tvEncontrado.visibility = View.GONE
            holder.tvOutrosMercados.visibility = View.GONE
            holder.btnVerOferta.visibility = View.GONE
            holder.btnVerOferta.setOnClickListener(null)
        }

        holder.btnDelete.setOnClickListener { onDelete(item) }
        holder.btnPreco.setOnClickListener { onSetPreco(item) }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView = view.findViewById(R.id.tvProdutoNome)
        val tvPrecoMax: TextView = view.findViewById(R.id.tvProdutoPrecoMax)
        val tvStatusComparacao: TextView = view.findViewById(R.id.tvStatusComparacao)
        val tvEncontrado: TextView = view.findViewById(R.id.tvProdutoEncontrado)
        val tvOutrosMercados: TextView = view.findViewById(R.id.tvOutrosMercados)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteProduto)
        val btnPreco: ImageButton = view.findViewById(R.id.btnSetPreco)
        val btnVerOferta: MaterialButton = view.findViewById(R.id.btnVerOfertaCarrinho)
    }
}
