package com.example.wofertas

import android.content.DialogInterface
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
import com.example.wofertas.network.OfertaDto
import com.example.wofertas.network.OfertaRequest
import kotlinx.coroutines.launch

/**
 * Tela de gerenciamento de ofertas para o Supermercado.
 * Corrigido: Revertido para listarMinhasOfertas para evitar Erro 500 e adicionado fallback por perfil.
 */
class GerenciarOfertasSupermercadoActivity : BaseMercadoActivity(),
    SupermercadoOfertaAdapter.OnItemActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SupermercadoOfertaAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMensagem: TextView

    private val api get() = ApiClient.authService(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gerenciar_ofertas_supermercado)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_gerenciar_ofertas)
        setSupportActionBar(toolbar)
        supportActionBar?.apply { 
            setDisplayHomeAsUpEnabled(true)
            title = "Gerenciar Ofertas" 
        }

        recyclerView = findViewById(R.id.recyclerViewSupermercadoOfertas)
        progressBar  = findViewById(R.id.progressBarSupermercadoOfertas)
        tvMensagem   = findViewById(R.id.tvNoOffersMessageSupermercado)

        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = SupermercadoOfertaAdapter(mutableListOf(), this, this)
        recyclerView.adapter = adapter

        carregarOfertas()
    }

    override fun onResume() { 
        super.onResume()
        carregarOfertas() 
    }

    private fun carregarOfertas() {
        progressBar.visibility = View.VISIBLE
        tvMensagem.visibility  = View.GONE

        lifecycleScope.launch {
            try {
                // Tenta o endpoint de histórico (My Offers para o Token atual)
                val resp = api.listarMinhasOfertas()
                
                if (resp.isSuccessful) {
                    val dtos = resp.body() ?: emptyList()
                    processarOfertas(dtos)
                } else {
                    // Se falhar ou der erro (como o 500), tentamos via Perfil -> ID do Mercado
                    tentarCarregarPorPerfil()
                }
            } catch (e: Exception) {
                Log.e("GerenciarOfertas", "Erro ao carregar", e)
                tvMensagem.text = "Sem conexão com o servidor."
                tvMensagem.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun tentarCarregarPorPerfil() {
        try {
            val perfilResp = api.getMercadoPerfil()
            if (perfilResp.isSuccessful) {
                val mercadoId = perfilResp.body()?.id
                if (mercadoId != null) {
                    val respAlt = api.listarOfertasPorMercado(mercadoId)
                    if (respAlt.isSuccessful) {
                        processarOfertas(respAlt.body() ?: emptyList())
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GerenciarOfertas", "Erro no fallback por perfil", e)
        }
        
        tvMensagem.text = "Erro ao carregar ofertas do servidor."
        tvMensagem.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    private fun processarOfertas(dtos: List<OfertaDto>) {
        val novasOfertas = dtos.map { dto ->
            Oferta().apply {
                ofertaId             = dto.id
                mercadoId            = dto.mercado?.id
                nome                 = dto.nome
                status               = dto.status ?: "ATIVO"
                dataValidade         = dto.data
                imagemOferta         = dto.imagemOferta ?: dto.imagem
                nomeSupermercado     = dto.mercado?.nome ?: AuthManager.getNome(this@GerenciarOfertasSupermercadoActivity)
                enderecoSupermercado = dto.mercado?.endereco
                imagemLogo           = dto.mercado?.imagemLogo
                latitude             = dto.mercado?.latitude
                longitude            = dto.mercado?.longitude
            }
        }
        
        adapter.setOfertas(novasOfertas)
        
        if (novasOfertas.isEmpty()) {
            tvMensagem.text = "Você ainda não publicou nenhuma oferta."
            tvMensagem.visibility = View.VISIBLE
        } else {
            tvMensagem.visibility = View.GONE
        }
        progressBar.visibility = View.GONE
    }

    override fun onPdfClick(oferta: Oferta) {
        val url = oferta.imagemOferta
        if (!url.isNullOrBlank()) {
            startActivity(Intent(this, VerPDF::class.java).apply {
                putExtra("pdfUrl",       url)
                putExtra("oferta_id",    oferta.ofertaId ?: "")
                putExtra("oferta_nome",  oferta.nome ?: "")
                putExtra("mercado_nome", oferta.nomeSupermercado ?: "")
            })
        } else {
            Toast.makeText(this, "Imagem não disponível.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEditClick(oferta: Oferta) {
        startActivity(Intent(this, DashboardSupermercadoActivity::class.java).apply {
            putExtra("ofertaParaEditar", oferta)
        })
    }

    override fun onDeleteClick(oferta: Oferta) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Oferta")
            .setMessage("Excluir '${oferta.nome}'?")
            .setPositiveButton("Excluir") { _, _ -> deletarOferta(oferta) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarOferta(oferta: Oferta) {
        val id = oferta.ofertaId ?: return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.deletarOferta(id)
                if (resp.isSuccessful) {
                    Toast.makeText(this@GerenciarOfertasSupermercadoActivity, "Excluída!", Toast.LENGTH_SHORT).show()
                    carregarOfertas()
                } else {
                    Toast.makeText(this@GerenciarOfertasSupermercadoActivity, "Erro ao excluir (${resp.code()})", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@GerenciarOfertasSupermercadoActivity, "Erro conexão", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onToggleStatusClick(oferta: Oferta) {
        val novoStatus = if (oferta.status?.uppercase() == "SUSPENSO") "ATIVO" else "SUSPENSO"
        val id = oferta.ofertaId ?: return
        lifecycleScope.launch {
            try {
                val request = OfertaRequest(oferta.nome ?: "", novoStatus, oferta.dataValidade ?: "", oferta.imagemOferta)
                if (api.atualizarOferta(id, request).isSuccessful) {
                    carregarOfertas()
                }
            } catch (e: Exception) {
                Log.e("GerenciarOfertas", "Erro ao mudar status", e)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { 
            finish()
            return true 
        }
        return super.onOptionsItemSelected(item)
    }
}
