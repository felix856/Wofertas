package com.example.wofertas.ui.publicar

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.BaseMercadoActivity
import com.example.wofertas.R
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.OfertaRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

/**
 * Tela de criação/edição de oferta com upload de imagem via Multipart.
 */
class PublicarOfertaActivity : BaseMercadoActivity() {

    private lateinit var etNome: EditText
    private lateinit var etStatus: EditText
    private lateinit var tvDataFim: TextView
    private lateinit var btnDataFim: View
    private lateinit var tvImagemNome: TextView
    private lateinit var btnSelecionarImagem: View
    private lateinit var btnPublicar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var ivPreview: ImageView

    private var selectedImageUri: Uri? = null
    private var ofertaId: String?     = null
    private var dataFim: String       = ""
    private var imagemAtual: String?  = null

    private val api get() = ApiClient.authService(this)

    private val selecionarImagem = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            tvImagemNome.text = "Nova imagem selecionada"
            tvImagemNome.visibility = View.VISIBLE
            ivPreview.setImageURI(uri)
            ivPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicar_oferta)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_publicar_oferta)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        etNome              = findViewById(R.id.etNomeOfertaPublicar)
        etStatus            = findViewById(R.id.etStatusOfertaPublicar)
        tvDataFim           = findViewById(R.id.tvDataFimPublicar)
        btnDataFim          = findViewById(R.id.btnSelecionarDataFim)
        tvImagemNome        = findViewById(R.id.tvImagemNomePublicar)
        btnSelecionarImagem = findViewById(R.id.btnSelecionarImagemPublicar)
        btnPublicar         = findViewById(R.id.btnPublicarOfertaSubmit)
        progressBar         = findViewById(R.id.progressBarPublicar)
        ivPreview           = findViewById(R.id.ivPreviewPublicar)

        ofertaId = intent.getStringExtra("oferta_id")
        if (ofertaId != null) {
            supportActionBar?.title = "Editar Oferta"
            btnPublicar.text = "Salvar alteracoes"
            etNome.setText(intent.getStringExtra("oferta_nome") ?: "")
            etStatus.setText(intent.getStringExtra("oferta_status") ?: "ATIVO")
            dataFim = intent.getStringExtra("oferta_data") ?: ""
            tvDataFim.text = dataFim
            imagemAtual = intent.getStringExtra("oferta_imagem")
            tvImagemNome.text = if (imagemAtual.isNullOrBlank()) {
                "Nenhuma imagem atual. Toque para escolher."
            } else {
                "Imagem atual mantida. Toque para trocar."
            }
        } else {
            supportActionBar?.title = "Publicar Oferta"
            btnPublicar.text = "Publicar Oferta"
            etStatus.setText("ATIVO")
        }

        btnDataFim.setOnClickListener   { abrirDatePicker { d -> dataFim = d; tvDataFim.text = d } }
        btnSelecionarImagem.setOnClickListener { selecionarImagem.launch("image/*") }
        btnPublicar.setOnClickListener  { validarEPublicar() }
    }

    private fun abrirDatePicker(onDate: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day -> onDate("%04d-%02d-%02d".format(year, month + 1, day)) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validarEPublicar() {
        val nome   = etNome.text.toString().trim()
        val status = etStatus.text.toString().trim().uppercase().ifEmpty { "ATIVO" }

        if (nome.isBlank()) { 
            etNome.error = "Informe o nome da oferta"
            return 
        }
        
        if (dataFim.isBlank()) {
            Toast.makeText(this, "Selecione uma data de validade", Toast.LENGTH_SHORT).show()
            return
        }

        // imagemOferta vai null no JSON, pois o upload da imagem física é via multipart depois
        if (selectedImageUri == null && imagemAtual.isNullOrBlank()) {
            Toast.makeText(this, "Selecione uma imagem da oferta", Toast.LENGTH_SHORT).show()
            return
        }

        val body = OfertaRequest(
            nome   = nome,
            status = status,
            data   = dataFim,
            imagemOferta = if (ofertaId != null) imagemAtual else null
        )

        setCarregando(true)

        lifecycleScope.launch {
            try {
                val response = if (ofertaId != null) {
                    api.atualizarOferta(ofertaId!!, body)
                } else {
                    api.criarOferta(body)
                }

                if (response.isSuccessful) {
                    val ofertaCriada = response.body()
                    if (ofertaCriada != null && selectedImageUri != null) {
                        executarUploadMultipart(ofertaCriada.id, selectedImageUri!!)
                    } else if (ofertaCriada != null) {
                        sucessoFinal()
                    } else {
                        Toast.makeText(this@PublicarOfertaActivity, "Erro ao processar resposta do servidor", Toast.LENGTH_LONG).show()
                        setCarregando(false)
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erro desconhecido"
                    Log.e("PublicarOferta", "Erro API: $errorMsg")
                    Toast.makeText(this@PublicarOfertaActivity, "Erro ao salvar: ${response.code()}", Toast.LENGTH_LONG).show()
                    setCarregando(false)
                }
            } catch (e: Exception) {
                Log.e("PublicarOferta", "Erro de conexão", e)
                Toast.makeText(this@PublicarOfertaActivity, "Sem conexão com o servidor", Toast.LENGTH_LONG).show()
                setCarregando(false)
            }
        }
    }

    private fun executarUploadMultipart(id: String, uri: Uri) {
        lifecycleScope.launch {
            try {
                val file = uriToFile(uri)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("foto", file.name, requestFile)

                val response = api.uploadImagemOferta(id, part)
                if (response.isSuccessful) {
                    sucessoFinal()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erro no upload"
                    Log.e("PublicarOferta", "Erro Upload: $errorMsg")
                    Toast.makeText(this@PublicarOfertaActivity, "Erro no upload da imagem", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("PublicarOferta", "Erro ao processar imagem", e)
                Toast.makeText(this@PublicarOfertaActivity, "Erro ao processar imagem", Toast.LENGTH_LONG).show()
            } finally {
                setCarregando(false)
            }
        }
    }

    private fun uriToFile(uri: Uri): File {
        val file = File(cacheDir, "temp_oferta_${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Nao foi possivel ler a imagem selecionada")
        return file
    }

    private fun setCarregando(carregando: Boolean) {
        progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
        btnPublicar.isEnabled  = !carregando
    }

    private fun sucessoFinal() {
        val msg = if (ofertaId != null) "Oferta atualizada com sucesso!" else "Oferta publicada com sucesso!"
        Toast.makeText(this@PublicarOfertaActivity, msg, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}
