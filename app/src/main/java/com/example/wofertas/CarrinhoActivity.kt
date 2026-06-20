package com.example.wofertas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
// herdado de BaseClienteActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wofertas.data.local.AppDatabase
import com.example.wofertas.data.local.entities.ProdutoEncontradoEntity
import com.example.wofertas.data.local.entities.ProdutoListaEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CarrinhoActivity : BaseClienteActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvVazio: TextView
    private lateinit var etNovoProduto: EditText
    private lateinit var btnAdicionar: ImageButton
    private lateinit var adapter: CarrinhoAdapter

    private val db by lazy { AppDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrinho)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_carrinho)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView    = findViewById(R.id.recyclerCarrinho)
        tvVazio         = findViewById(R.id.tvCarrinhoVazio)
        etNovoProduto   = findViewById(R.id.etNovoProduto)
        btnAdicionar    = findViewById(R.id.btnAdicionarProduto)

        adapter = CarrinhoAdapter(
            onDelete = { item -> confirmarExclusao(item) },
            onSetPreco = { item -> dialogPrecoMaximo(item) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAdicionar.setOnClickListener { adicionarProduto() }

        // Observa lista em tempo real
        lifecycleScope.launch {
            db.produtoListaDao().getAllFlow().collectLatest { lista ->
                tvVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                // Enriquecer com resultados OCR
                val encontrados = db.produtoEncontradoDao().getAll()
                adapter.atualizar(lista, encontrados)
            }
        }
    }

    private fun adicionarProduto() {
        val nome = etNovoProduto.text.toString().trim()
        if (nome.isBlank()) { etNovoProduto.error = "Digite o produto"; return }
        lifecycleScope.launch {
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
            hint = "Ex: 15.90 (deixe vazio para qualquer preço)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item.precoMaximo?.toString() ?: "")
            setPadding(40, 20, 40, 20)
        }
        AlertDialog.Builder(this)
            .setTitle("Preço máximo para \"${item.nome}\"")
            .setView(et)
            .setPositiveButton("Salvar") { _, _ ->
                val preco = et.text.toString().toDoubleOrNull()
                lifecycleScope.launch {
                    db.produtoListaDao().insert(item.copy(precoMaximo = preco))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class CarrinhoAdapter(
    private val onDelete: (ProdutoListaEntity) -> Unit,
    private val onSetPreco: (ProdutoListaEntity) -> Unit
) : RecyclerView.Adapter<CarrinhoAdapter.VH>() {

    private val items      = mutableListOf<ProdutoListaEntity>()
    private val encontrados = mutableListOf<ProdutoEncontradoEntity>()

    fun atualizar(lista: List<ProdutoListaEntity>, enc: List<ProdutoEncontradoEntity>) {
        items.clear(); items.addAll(lista)
        encontrados.clear(); encontrados.addAll(enc)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_carrinho, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNome.text = item.nome

        // Preço máximo configurado
        holder.tvPrecoMax.text = item.precoMaximo
            ?.let { "Máx: R$ %.2f".format(it) } ?: ""
        holder.tvPrecoMax.visibility =
            if (item.precoMaximo != null) View.VISIBLE else View.GONE

        // Resultados OCR para este produto
        val matches = encontrados.filter { enc ->
            OcrHelper.corresponde(item.nome, enc.nomeProduto) &&
                    (item.precoMaximo == null || enc.preco == null || enc.preco <= item.precoMaximo)
        }.sortedBy { it.preco }

        if (matches.isNotEmpty()) {
            val melhor = matches.first()
            val precoStr = melhor.preco?.let { " · R$ %.2f".format(it) } ?: ""
            holder.tvEncontrado.text = "📍 ${melhor.nomeMercado}$precoStr"
            holder.tvEncontrado.visibility = View.VISIBLE
            // Mostra outros mercados se houver
            if (matches.size > 1) {
                holder.tvOutrosMercados.text = "+${matches.size - 1} mercado(s) com este produto"
                holder.tvOutrosMercados.visibility = View.VISIBLE
            } else {
                holder.tvOutrosMercados.visibility = View.GONE
            }
        } else {
            holder.tvEncontrado.visibility = View.GONE
            holder.tvOutrosMercados.visibility = View.GONE
        }

        holder.btnDelete.setOnClickListener   { onDelete(item) }
        holder.btnPreco.setOnClickListener    { onSetPreco(item) }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome          : TextView    = view.findViewById(R.id.tvProdutoNome)
        val tvPrecoMax      : TextView    = view.findViewById(R.id.tvProdutoPrecoMax)
        val tvEncontrado    : TextView    = view.findViewById(R.id.tvProdutoEncontrado)
        val tvOutrosMercados: TextView    = view.findViewById(R.id.tvOutrosMercados)
        val btnDelete       : ImageButton = view.findViewById(R.id.btnDeleteProduto)
        val btnPreco        : ImageButton = view.findViewById(R.id.btnSetPreco)
    }
}