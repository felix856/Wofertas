package com.example.wofertas

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.wofertas.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var editNome: EditText
    private lateinit var editEndereco: EditText
    private lateinit var btnSalvar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var imgPerfil: ImageView
    private lateinit var btnTrocarFoto: Button

    private var novaImagemBase64: String? = null
    private val api get() = ApiClient.authService(this)

    // Seletor de fotos moderno (Photo Picker)
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Processa a imagem em uma Coroutine para não travar a UI
            lifecycleScope.launch {
                setLoading(true)
                val imagemProcessada = processarEComprimirImagem(uri)
                if (imagemProcessada == null) {
                    setLoading(false)
                    Toast.makeText(this@EditarPerfilActivity, "Nao foi possivel ler esta imagem.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                novaImagemBase64 = imagemProcessada

                // Carrega a prévia usando Coil com efeito circular
                imgPerfil.load(uri) {
                    transformations(CircleCropTransformation())
                }
                setLoading(false)
                Toast.makeText(this@EditarPerfilActivity, "Foto processada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        if (!AuthManager.isLoggedIn(this)) {
            irParaLogin()
            return
        }

        ChatbotLauncher.install(this)

        configurarInterface()
        carregarDadosAtuais()

        btnSalvar.setOnClickListener { salvar() }
        btnTrocarFoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun configurarInterface() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_editar_perfil)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        editNome = findViewById(R.id.editNome)
        editEndereco = findViewById(R.id.editEndereco)
        btnSalvar = findViewById(R.id.btnSalvar)
        progressBar = findViewById(R.id.progressBarEditar)
        imgPerfil = findViewById(R.id.imgPerfilEditar)
        btnTrocarFoto = findViewById(R.id.btnTrocarFoto)

        if (!AuthManager.isMercado(this)) {
            editEndereco.visibility = View.GONE
        }
    }

    private fun carregarDadosAtuais() {
        val userId = AuthManager.getUserId(this) ?: return
        lifecycleScope.launch {
            try {
                if (AuthManager.isMercado(this@EditarPerfilActivity)) {
                    val resp = api.getMercado(userId)
                    if (resp.isSuccessful) {
                        resp.body()?.let {
                            editNome.setText(it.nome)
                            editEndereco.setText(it.endereco ?: "")
                            // DESENVOLVIDO: Carregamento de imagem atual com Coil
                            carregarImagemComCoil(it.imagemLogo)
                        }
                    }
                } else {
                    val resp = api.getUsuario(userId)
                    if (resp.isSuccessful) {
                        resp.body()?.let {
                            editNome.setText(it.nome)
                            // DESENVOLVIDO: Carregamento de imagem atual com Coil
                            carregarImagemComCoil(it.imagemPerfil)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar dados", e)
            }
        }
    }

    private fun carregarImagemComCoil(imagem: String?) {
        if (imagem.isNullOrBlank()) return

        val source = if (imagem.startsWith("data:image")) imagem else imagem // Lida com Base64 ou URL
        imgPerfil.load(source) {
            crossfade(true)
            placeholder(R.drawable.placeholder_user) // Crie este drawable
            transformations(CircleCropTransformation())
        }
    }

    // DESENVOLVIDO: Compressão de imagem para poupar banda e respeitar limite do servidor
    private suspend fun processarEComprimirImagem(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return@withContext null

            // Redimensiona se for maior que 1024px para manter performance
            val scale = 1024f / Math.max(originalBitmap.width, originalBitmap.height).coerceAtLeast(1)
            val bitmapFinal = if (scale < 1) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else originalBitmap

            val out = ByteArrayOutputStream()
            bitmapFinal.compress(Bitmap.CompressFormat.JPEG, 70, out) // 70% de qualidade é o ideal
            val bytes = out.toByteArray()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao processar imagem", e)
            null
        }
    }

    private fun salvar() {
        val nome = editNome.text.toString().trim()
        if (TextUtils.isEmpty(nome)) { editNome.error = "Obrigatório"; return }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val payload = mutableMapOf<String, String>()
                payload["nome"] = nome

                if (AuthManager.isMercado(this@EditarPerfilActivity)) {
                    payload["endereco"] = editEndereco.text.toString().trim()
                    novaImagemBase64?.let { payload["imagemLogo"] = it }
                    val resp = api.atualizarMercadoRequest(payload)
                    tratarResposta(resp.isSuccessful, resp.code(), nome)
                } else {
                    novaImagemBase64?.let { payload["imagemPerfil"] = it }
                    val resp = api.atualizarPerfil(payload)
                    tratarResposta(resp.isSuccessful, resp.code(), nome)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar", e)
                Toast.makeText(this@EditarPerfilActivity, "Erro de conexão", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun tratarResposta(sucesso: Boolean, codigo: Int, novoNome: String) {
        if (sucesso) {
            // DESENVOLVIDO: Atualiza dados no AuthManager local para refletir no App na hora
            AuthManager.updateLocalProfile(this, novoNome)
            Toast.makeText(this, "Perfil atualizado!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Erro ao salvar: $codigo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSalvar.isEnabled = !loading
        btnTrocarFoto.isEnabled = !loading
    }

    private fun irParaLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    companion object { private const val TAG = "EditarPerfil" }
}
