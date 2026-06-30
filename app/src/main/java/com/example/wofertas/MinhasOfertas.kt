package com.example.wofertas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wofertas.network.ApiClient
import com.example.wofertas.ui.publicar.PublicarOfertaActivity
import com.example.wofertas.ui.encartes.EncartesActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

/**
 * Gerenciamento de ofertas pelo Supermercado.
 * Permite visualizar, editar e excluir ofertas próprias.
 */
class MinhasOfertas : BaseMercadoActivity(), SupermercadoOfertaAdapter.OnItemActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var ofertaAdapter: SupermercadoOfertaAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMessage: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    private val listaOfertas: MutableList<Oferta> = mutableListOf()
    private val api get() = ApiClient.authService(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minhas_ofertas)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_minhas_ofertas)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Minhas Ofertas"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView          = findViewById(R.id.recyclerViewMinhasOfertas)
        progressBar           = findViewById(R.id.progressBarMinhasOfertas)
        tvMessage             = findViewById(R.id.tvMinhasOfertasMessage)
        bottomNavigationView  = findViewById(R.id.bottom_navigation_supermercado)

        // IMPORTANTE: Usando SupermercadoOfertaAdapter para ter botões de Edição/Exclusão
        ofertaAdapter = SupermercadoOfertaAdapter(mutableListOf(), this, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ofertaAdapter

        configurarBottomNav()
        carregarOfertas()
    }

    override fun onResume() {
        super.onResume()
        carregarOfertas()
    }

    private fun carregarOfertas() {
        progressBar.visibility = View.VISIBLE
        tvMessage.visibility   = View.GONE

        lifecycleScope.launch {
            try {
                val resp = api.listarMinhasOfertas()
                if (resp.isSuccessful) {
                    val dtos = resp.body() ?: emptyList()
                    listaOfertas.clear()
                    listaOfertas.addAll(dtos.map { dto ->
                        Oferta().apply {
                            ofertaId         = dto.id
                            mercadoId        = dto.mercado?.id ?: dto.mercadoId
                            nome             = dto.nome
                            status           = dto.status
                            dataValidade     = dto.data
                            imagemOferta     = dto.imagemOferta ?: dto.imagem
                            nomeSupermercado = dto.mercado?.nome
                        }
                    })
                    ofertaAdapter.setOfertas(listaOfertas)

                    tvMessage.visibility = if (listaOfertas.isEmpty()) {
                        tvMessage.text = "Você ainda não publicou nenhuma oferta."
                        View.VISIBLE
                    } else View.GONE

                } else if (resp.code() == 401) {
                    tratarSessaoExpirada()
                } else {
                    tvMessage.text = "Erro ao carregar (${resp.code()})."
                    tvMessage.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("MinhasOfertas", "Erro ao carregar ofertas", e)
                tvMessage.text = "Sem conexão com o servidor."
                tvMessage.visibility = View.VISIBLE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    // --- AÇÕES DO ADAPTER ---

    override fun onPdfClick(oferta: Oferta) {
        if (!oferta.imagemOferta.isNullOrEmpty()) {
            startActivity(Intent(this, VerPDF::class.java).apply {
                putExtra("pdfUrl",       oferta.imagemOferta)
                putExtra("oferta_id",    oferta.ofertaId)
                putExtra("oferta_nome",  oferta.nome)
            })
        }
    }

    override fun onEditClick(oferta: Oferta) {
        startActivity(Intent(this, PublicarOfertaActivity::class.java).apply {
            putExtra("oferta_id",     oferta.ofertaId)
            putExtra("oferta_nome",   oferta.nome)
            putExtra("oferta_status", oferta.status)
            putExtra("oferta_data",   oferta.dataValidade)
        })
    }

    override fun onDeleteClick(oferta: Oferta) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Oferta")
            .setMessage("Tem certeza que deseja excluir '${oferta.nome}'?")
            .setPositiveButton("Excluir") { _, _ -> deletarOfertaApi(oferta) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onToggleStatusClick(oferta: Oferta) {
        // Implementação futura para Ativar/Desativar rapidamente
        Toast.makeText(this, "Funcionalidade de status em breve", Toast.LENGTH_SHORT).show()
    }

    private fun deletarOfertaApi(oferta: Oferta) {
        val id = oferta.ofertaId ?: return
        lifecycleScope.launch {
            try {
                val resp = api.deletarOferta(id)
                if (resp.isSuccessful) {
                    Toast.makeText(this@MinhasOfertas, "Oferta excluída!", Toast.LENGTH_SHORT).show()
                    carregarOfertas()
                } else {
                    Toast.makeText(this@MinhasOfertas, "Erro ao excluir", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MinhasOfertas, "Erro de conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tratarSessaoExpirada() {
        AuthManager.clearSession(this)
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun configurarBottomNav() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, DashboardSupermercadoActivity::class.java))
                    finish(); true
                }
                R.id.navigation_minhas_ofertas -> true
                R.id.navigation_criar_oferta -> {
                    startActivity(Intent(this, PublicarOfertaActivity::class.java))
                    true
                }
                R.id.navigation_gerenciar_ofertas_supermercado -> true
                R.id.navigation_encartes -> {
                    abrirMeusEncartes()
                    true
                }
                R.id.navigation_perfil_supermercado -> {
                    startActivity(Intent(this, Perfil::class.java)); true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.navigation_minhas_ofertas
    }

    private fun abrirMeusEncartes() {
        val mercadoId = AuthManager.getUserId(this) ?: return
        startActivity(Intent(this, EncartesActivity::class.java).apply {
            putExtra("mercado_id", mercadoId)
            putExtra("mercado_nome", AuthManager.getNome(this@MinhasOfertas) ?: "Meus encartes")
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
