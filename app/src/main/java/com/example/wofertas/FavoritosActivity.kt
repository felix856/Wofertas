package com.example.wofertas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wofertas.utils.loadImage
import com.example.wofertas.data.local.entities.FavoritoEntity
import com.example.wofertas.viewmodels.FavoritosUiState
import com.example.wofertas.viewmodels.FavoritosViewModel
import com.example.wofertas.viewmodels.ToggleEvento
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import com.example.wofertas.ui.encartes.EncartesActivity

/**
 * Tela de supermercados salvos pelo cliente.
 *
 * Usa FavoritosViewModel + FavoritoRepository (já existentes e bem implementados).
 * Funciona offline: exibe o cache Room quando não há rede.
 *
 * Exclusiva de clientes — herda BaseClienteActivity.
 */
class FavoritosActivity : BaseClienteActivity() {

    private lateinit var recyclerView:   RecyclerView
    private lateinit var progressBar:    ProgressBar
    private lateinit var tvMessage:      TextView
    private lateinit var bottomNav:      BottomNavigationView
    private lateinit var adapter:        FavoritosAdapter

    private val viewModel: FavoritosViewModel by viewModels {
        FavoritosViewModel.Factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BaseClienteActivity.onCreate() já verificou login e tipo — se chegou aqui, ok
        setContentView(R.layout.activity_favoritos)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_favoritos)
        setSupportActionBar(toolbar)
        // Removido supportActionBar?.title pois já definimos no XML da Toolbar
        
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerViewFavoritos)
        progressBar  = findViewById(R.id.progressBarFavoritos)
        tvMessage    = findViewById(R.id.tvFavoritosMessage)
        bottomNav    = findViewById(R.id.bottom_navigation)

        adapter = FavoritosAdapter(
            onRemover = { fav -> viewModel.toggleFavorito(fav.mercadoId) },
            onAbrir   = { fav -> abrirEncartesMercado(fav) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        configurarBottomNav()
        observarViewModel()
    }

    // ── ViewModel ─────────────────────────────────────────────────────────────

    private fun observarViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                progressBar.visibility = View.GONE
                when (state) {
                    is FavoritosUiState.Loading -> progressBar.visibility = View.VISIBLE
                    is FavoritosUiState.Vazio   -> mostrarMensagem("Você ainda não salvou nenhum supermercado.\n\nToque no ♡ em qualquer oferta para salvar.")
                    is FavoritosUiState.Success -> {
                        tvMessage.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.atualizar(state.favoritos)
                    }
                    is FavoritosUiState.Error -> mostrarMensagem("Erro ao carregar favoritos. Tente novamente.")
                }
            }
        }

        lifecycleScope.launch {
            viewModel.toggleEvento.collect { evento ->
                when (evento) {
                    is ToggleEvento.Sucesso -> {
                        val msg = if (evento.agora) "Salvo!" else "Removido dos salvos"
                        Toast.makeText(this@FavoritosActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    is ToggleEvento.Erro -> Toast.makeText(
                        this@FavoritosActivity,
                        "Erro ao atualizar. Tente novamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun mostrarMensagem(msg: String) {
        recyclerView.visibility = View.GONE
        tvMessage.text = msg
        tvMessage.visibility = View.VISIBLE
    }

    // ── Navegação ─────────────────────────────────────────────────────────────

    private fun abrirEncartesMercado(fav: FavoritoEntity) {
        // Abre encartes do mercado (EncartesActivity já existe)
        startActivity(Intent(this, EncartesActivity::class.java).apply {
            putExtra("mercado_id",   fav.mercadoId)
            putExtra("mercado_nome", fav.mercadoNome)
        })
    }

    private fun configurarBottomNav() {
        bottomNav.selectedItemId = R.id.navigation_salvos
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, ListaOfertas::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }); finish(); true
                }
                R.id.navigation_mapa   -> { startActivity(Intent(this, Mapa::class.java)); true }
                R.id.navigation_carrinho -> { startActivity(Intent(this, CarrinhoActivity::class.java)); true }
                R.id.navigation_salvos -> true   // já está aqui
                R.id.navigation_perfil -> { startActivity(Intent(this, Perfil::class.java)); true }
                else -> false
            }
        }
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class FavoritosAdapter(
    private val onRemover: (FavoritoEntity) -> Unit,
    private val onAbrir:   (FavoritoEntity) -> Unit
) : RecyclerView.Adapter<FavoritosAdapter.VH>() {

    private val items = mutableListOf<FavoritoEntity>()

    fun atualizar(lista: List<FavoritoEntity>) {
        items.clear()
        items.addAll(lista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_favorito, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val fav = items[position]

        holder.tvNome.text     = fav.mercadoNome.ifBlank { "Supermercado" }
        holder.tvEndereco.text = fav.mercadoEndereco ?: ""
        holder.tvEndereco.visibility =
            if (fav.mercadoEndereco.isNullOrBlank()) View.GONE else View.VISIBLE

        holder.imgLogo.loadImage(fav.mercadoImagemLogo, R.drawable.logo_supermercado_placeholder)

        holder.btnRemover.setOnClickListener { onRemover(fav) }
        holder.itemView.setOnClickListener   { onAbrir(fav) }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgLogo:   ImageView = view.findViewById(R.id.imgFavoritoLogo)
        val tvNome:    TextView  = view.findViewById(R.id.tvFavoritoNome)
        val tvEndereco: TextView = view.findViewById(R.id.tvFavoritoEndereco)
        val btnRemover: ImageView = view.findViewById(R.id.btnRemoverFavorito)
    }
}