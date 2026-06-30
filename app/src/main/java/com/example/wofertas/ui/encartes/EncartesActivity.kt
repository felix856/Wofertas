package com.example.wofertas.ui.encartes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wofertas.AuthManager
import com.example.wofertas.ChatbotLauncher
import com.example.wofertas.R
import com.example.wofertas.VerPDF
import com.example.wofertas.data.repository.EncarteRepository
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.EncarteDto
import kotlinx.coroutines.launch

class EncartesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMensagem: TextView
    private lateinit var btnUpload: Button

    private lateinit var adapter: EncartesAdapter
    private lateinit var repository: EncarteRepository

    private var mercadoId: String = ""
    private var mercadoNome: String = ""
    private var pdfUri: Uri? = null
    private var editPdfUri: Uri? = null
    private var editPdfLabel: TextView? = null

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pdfUri = uri
            mostrarDialogTitulo()
        }
    }

    private val pickEditPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            editPdfUri = uri
            editPdfLabel?.text = "Novo PDF selecionado"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encartes)

        mercadoId = intent.getStringExtra("mercado_id") ?: run { finish(); return }
        mercadoNome = intent.getStringExtra("mercado_nome") ?: "Encartes"

        ChatbotLauncher.install(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_encartes)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = mercadoNome
        }
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerEncartes)
        progressBar = findViewById(R.id.progressEncartes)
        tvMensagem = findViewById(R.id.tvEncartesVazio)
        btnUpload = findViewById(R.id.btnUploadEncarte)

        repository = EncarteRepository(this)

        val isMercadoOwner = AuthManager.isMercado(this) &&
                AuthManager.getUserId(this) == mercadoId

        adapter = EncartesAdapter(
            onVerClick = { abrirEncarte(it) },
            onEditClick = if (isMercadoOwner) { encarte -> mostrarDialogEditar(encarte) } else null,
            onDeleteClick = if (isMercadoOwner) { encarte -> confirmarExclusao(encarte) } else null
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnUpload.visibility = if (isMercadoOwner) View.VISIBLE else View.GONE
        btnUpload.setOnClickListener { pickPdf.launch("application/pdf") }

        carregarEncartes()
    }

    private fun carregarEncartes() {
        progressBar.visibility = View.VISIBLE
        tvMensagem.visibility = View.GONE

        lifecycleScope.launch {
            val result = repository.fetchEncartes(mercadoId)
            progressBar.visibility = View.GONE

            result.onSuccess { lista ->
                adapter.submitList(lista)
                if (lista.isEmpty()) {
                    tvMensagem.text = "Nenhum encarte disponivel"
                    tvMensagem.visibility = View.VISIBLE
                } else {
                    tvMensagem.visibility = View.GONE
                }
            }.onFailure {
                tvMensagem.text = "Erro ao carregar encartes"
                tvMensagem.visibility = View.VISIBLE
            }
        }
    }

    private fun abrirEncarte(encarte: EncarteDto) {
        var urlFinal = encarte.urlPdf
        if (urlFinal.startsWith("/") || !urlFinal.startsWith("http")) {
            val baseUrl = ApiClient.getCurrentBaseUrl().removeSuffix("/")
            val path = if (urlFinal.startsWith("/")) urlFinal else "/$urlFinal"
            urlFinal = baseUrl + path
        }

        startActivity(
            Intent(this, VerPDF::class.java).apply {
                putExtra("pdfUrl", urlFinal)
                putExtra("oferta_id", encarte.id)
                putExtra("oferta_nome", encarte.titulo)
                putExtra("mercado_nome", mercadoNome)
            }
        )
    }

    private fun mostrarDialogTitulo() {
        val input = EditText(this).apply {
            hint = "Titulo do encarte (ex: Ofertas da Semana)"
            setPadding(40, 20, 40, 20)
        }
        AlertDialog.Builder(this)
            .setTitle("Nome do encarte")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val titulo = input.text.toString().trim()
                if (titulo.isBlank()) Toast.makeText(this, "Informe um titulo", Toast.LENGTH_SHORT).show()
                else fazerUpload(titulo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogEditar(encarte: EncarteDto) {
        editPdfUri = null

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 16, 40, 0)
        }
        val inputTitulo = EditText(this).apply {
            hint = "Titulo do encarte"
            setText(encarte.titulo)
            setSelection(text.length)
        }
        val btnTrocarPdf = Button(this).apply {
            text = "Selecionar novo PDF (opcional)"
            isAllCaps = false
            setOnClickListener { pickEditPdf.launch("application/pdf") }
        }
        val tvPdfSelecionado = TextView(this).apply {
            text = "Mantendo PDF atual"
            setPadding(0, 8, 0, 0)
        }
        editPdfLabel = tvPdfSelecionado

        container.addView(inputTitulo)
        container.addView(btnTrocarPdf)
        container.addView(tvPdfSelecionado)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar encarte")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val titulo = inputTitulo.text.toString().trim()
                if (titulo.isBlank()) {
                    inputTitulo.error = "Informe um titulo"
                    return@setOnClickListener
                }
                dialog.dismiss()
                atualizarEncarte(encarte, titulo, editPdfUri)
            }
        }
        dialog.setOnDismissListener { editPdfLabel = null }
        dialog.show()
    }

    private fun fazerUpload(titulo: String) {
        val uri = pdfUri ?: return
        progressBar.visibility = View.VISIBLE
        btnUpload.isEnabled = false

        lifecycleScope.launch {
            val result = repository.uploadEncarte(mercadoId, titulo, uri)
            progressBar.visibility = View.GONE
            btnUpload.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@EncartesActivity, "Encarte enviado!", Toast.LENGTH_SHORT).show()
                carregarEncartes()
            }.onFailure { e ->
                Toast.makeText(this@EncartesActivity, "Falha no upload: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun atualizarEncarte(encarte: EncarteDto, titulo: String, novoPdf: Uri?) {
        progressBar.visibility = View.VISIBLE
        btnUpload.isEnabled = false

        lifecycleScope.launch {
            val result = repository.updateEncarte(encarte.id, titulo, novoPdf)
            progressBar.visibility = View.GONE
            btnUpload.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@EncartesActivity, "Encarte atualizado!", Toast.LENGTH_SHORT).show()
                carregarEncartes()
            }.onFailure { e ->
                Toast.makeText(this@EncartesActivity, "Erro ao atualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmarExclusao(encarte: EncarteDto) {
        AlertDialog.Builder(this)
            .setTitle("Excluir encarte")
            .setMessage("Deseja excluir \"${encarte.titulo}\"?")
            .setPositiveButton("Excluir") { _, _ -> excluirEncarte(encarte) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirEncarte(encarte: EncarteDto) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.deleteEncarte(encarte.id)
            progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(this@EncartesActivity, "Encarte excluido", Toast.LENGTH_SHORT).show()
                carregarEncartes()
            }.onFailure { e ->
                Toast.makeText(this@EncartesActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
