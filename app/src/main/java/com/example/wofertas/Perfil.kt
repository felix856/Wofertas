package com.example.wofertas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.PrivacyDeletionRequest
import com.example.wofertas.utils.Constants
import com.example.wofertas.utils.loadImage
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class Perfil : AppCompatActivity() {

    private lateinit var imageViewAvatar: ImageView
    private lateinit var textViewNomePerfil: TextView
    private lateinit var textViewEmailPerfil: TextView
    private lateinit var btnEditarPerfil: Button
    private lateinit var btnTrocarSenha: Button
    private lateinit var btnPrivacidade: Button
    private lateinit var btnSolicitarExclusao: Button
    private lateinit var btnSair: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    private val api get() = ApiClient.authService(this)

    // Seletor de imagem da galeria
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let { uploadFoto(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        if (!AuthManager.isLoggedIn(this)) {
            irParaLogin()
            return
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar_perfil)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Meu Perfil"
        }

        toolbar.setNavigationOnClickListener { finish() }

        imageViewAvatar = findViewById(R.id.imageViewAvatar)
        textViewNomePerfil = findViewById(R.id.textViewNomePerfil)
        textViewEmailPerfil = findViewById(R.id.textViewEmailPerfil)
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil)
        btnTrocarSenha = findViewById(R.id.btnTrocarSenha)
        btnPrivacidade = findViewById(R.id.btnPrivacidade)
        btnSolicitarExclusao = findViewById(R.id.btnSolicitarExclusao)
        btnSair = findViewById(R.id.btnSair)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        imageViewAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        configurarBottomNavPorTipo()
        configurarBotoes()
        carregarPerfil()
    }

    private fun uploadFoto(uri: Uri) {
        val userId = AuthManager.getUserId(this) ?: return
        
        lifecycleScope.launch {
            try {
                val file = uriToFile(uri)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("foto", file.name, requestFile)
                val logoBody = MultipartBody.Part.createFormData("logo", file.name, requestFile)

                val response = if (AuthManager.isMercado(this@Perfil)) {
                    api.uploadLogoMercado(userId, logoBody)
                } else {
                    api.uploadFotoUsuario(userId, body)
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@Perfil, "Foto atualizada!", Toast.LENGTH_SHORT).show()
                    carregarPerfil()
                } else {
                    Toast.makeText(this@Perfil, "Erro ao subir foto", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Perfil", "Erro upload", e)
                Toast.makeText(this@Perfil, "Erro de conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_profile_image.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        return file
    }

    override fun onResume() {
        super.onResume()
        if (!AuthManager.isLoggedIn(this)) {
            irParaLogin()
            return
        }
        carregarPerfil()
    }

    private fun configurarBottomNavPorTipo() {
        if (AuthManager.isMercado(this)) {
            configurarBottomNavMercado()
        } else {
            configurarBottomNavCliente()
        }
    }

    private fun configurarBottomNavCliente() {
        bottomNavigationView.menu.clear()
        bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu)
        bottomNavigationView.selectedItemId = R.id.navigation_perfil
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, ListaOfertas::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }); finish(); true
                }
                R.id.navigation_mapa -> { startActivity(Intent(this, Mapa::class.java)); true }
                R.id.navigation_carrinho -> { startActivity(Intent(this, CarrinhoActivity::class.java)); true }
                R.id.navigation_salvos -> { startActivity(Intent(this, FavoritosActivity::class.java)); true }
                R.id.navigation_perfil -> true
                else -> false
            }
        }
    }

    private fun configurarBottomNavMercado() {
        bottomNavigationView.menu.clear()
        bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_supermercado)
        bottomNavigationView.selectedItemId = R.id.navigation_perfil_supermercado
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, DashboardSupermercadoActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }); finish(); true
                }
                R.id.navigation_minhas_ofertas, R.id.navigation_gerenciar_ofertas_supermercado -> {
                    startActivity(Intent(this, GerenciarOfertasSupermercadoActivity::class.java)); true
                }
                R.id.navigation_perfil_supermercado -> true
                else -> false
            }
        }
    }

    private fun configurarBotoes() {
        btnSair.setOnClickListener {
            AuthManager.clearSession(this)
            irParaLogin()
        }
        btnTrocarSenha.setOnClickListener {
            startActivity(Intent(this, TrocarSenhaActivity::class.java))
        }
        btnEditarPerfil.setOnClickListener {
            startActivity(Intent(this, EditarPerfilActivity::class.java))
        }
        btnPrivacidade.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_POLICY_URL)))
        }
        btnSolicitarExclusao.setOnClickListener {
            confirmarSolicitacaoExclusao()
        }
    }

    private fun confirmarSolicitacaoExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Solicitar exclusao da conta?")
            .setMessage("Vamos registrar uma solicitacao para excluir sua conta e dados associados. Alguns dados podem ser retidos quando houver obrigacao legal, seguranca ou prevencao de fraude.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Solicitar") { _, _ -> solicitarExclusaoConta() }
            .show()
    }

    private fun solicitarExclusaoConta() {
        val email = AuthManager.getEmail(this)
        val tipo = AuthManager.getTipo(this)

        lifecycleScope.launch {
            try {
                btnSolicitarExclusao.isEnabled = false
                val response = api.solicitarExclusaoConta(
                    PrivacyDeletionRequest(
                        email = email,
                        requesterType = tipo,
                        reason = "Solicitado pelo app Android",
                        source = "ANDROID"
                    )
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@Perfil, "Solicitacao de exclusao registrada.", Toast.LENGTH_LONG).show()
                } else if (response.code() == 401) {
                    tratarSessaoExpirada()
                } else {
                    Toast.makeText(this@Perfil, "Nao foi possivel registrar agora.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("Perfil", "Erro ao solicitar exclusao", e)
                Toast.makeText(this@Perfil, "Falha de conexao ao solicitar exclusao.", Toast.LENGTH_LONG).show()
            } finally {
                btnSolicitarExclusao.isEnabled = true
            }
        }
    }

    private fun carregarPerfil() {
        val userId = AuthManager.getUserId(this) ?: return
        lifecycleScope.launch {
            try {
                if (AuthManager.isMercado(this@Perfil)) {
                    val resp = api.getMercado(userId)
                    if (resp.isSuccessful) {
                        val m = resp.body() ?: return@launch
                        textViewNomePerfil.text = m.nome
                        textViewEmailPerfil.text = m.email
                        btnEditarPerfil.visibility = View.VISIBLE
                        imageViewAvatar.loadImage(m.imagemLogo, R.drawable.logo_supermercado_placeholder)
                    } else if (resp.code() == 401) tratarSessaoExpirada()
                    else mostrarDadosBasicos()
                } else {
                    val resp = api.getUsuario(userId)
                    if (resp.isSuccessful) {
                        val u = resp.body() ?: return@launch
                        textViewNomePerfil.text = u.nome
                        textViewEmailPerfil.text = u.email
                        btnEditarPerfil.visibility = View.GONE
                        if (!u.imagemPerfil.isNullOrBlank()) {
                            imageViewAvatar.loadImage(u.imagemPerfil, R.drawable.perfil)
                        } else {
                            imageViewAvatar.setImageResource(R.drawable.perfil)
                        }
                    } else if (resp.code() == 401) tratarSessaoExpirada()
                    else mostrarDadosBasicos()
                }
            } catch (e: Exception) {
                Log.e("Perfil", "Erro ao carregar perfil", e)
                mostrarDadosBasicos()
            }
        }
    }

    private fun mostrarDadosBasicos() {
        textViewEmailPerfil.text = AuthManager.getEmail(this) ?: ""
        textViewNomePerfil.text = ""
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun tratarSessaoExpirada() {
        AuthManager.clearSession(this)
        Toast.makeText(this, "Sessão expirada.", Toast.LENGTH_LONG).show()
        irParaLogin()
    }

    private fun irParaLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }); finish()
    }
}
