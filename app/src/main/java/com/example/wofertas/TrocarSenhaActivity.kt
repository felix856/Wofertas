package com.example.wofertas

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.network.ApiErrorParser
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.TrocarSenhaRequest
import kotlinx.coroutines.launch

/**
 * Tela de troca de senha — disponível para USUARIO e MERCADO.
 *
 * Fluxo:
 *   1. Usuário informa senha atual + nova senha + confirmação
 *   2. Valida localmente (nova == confirmação, mínimo 6 chars)
 *   3. PUT /usuarios/{id}/senha  ou  PUT /mercados/{id}/senha
 *   4. Sucesso → volta para Perfil
 *
 * Herda AppCompatActivity diretamente (não BaseCliente nem BaseMercado)
 * porque é compartilhada entre os dois tipos.
 */
class TrocarSenhaActivity : AppCompatActivity() {

    private lateinit var editSenhaAtual:     EditText
    private lateinit var editNovaSenha:      EditText
    private lateinit var editConfirmarSenha: EditText
    private lateinit var btnSalvar:          Button
    private lateinit var progressBar:        ProgressBar

    private val api get() = ApiClient.authService(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trocar_senha)

        if (!AuthManager.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish(); return
        }

        ChatbotLauncher.install(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_trocar_senha)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        editSenhaAtual     = findViewById(R.id.editSenhaAtual)
        editNovaSenha      = findViewById(R.id.editNovaSenha)
        editConfirmarSenha = findViewById(R.id.editConfirmarSenha)
        btnSalvar          = findViewById(R.id.btnSalvarSenha)
        progressBar        = findViewById(R.id.progressBarTrocarSenha)

        btnSalvar.setOnClickListener { trocarSenha() }
    }

    private fun trocarSenha() {
        val atual       = editSenhaAtual.text.toString().trim()
        val nova        = editNovaSenha.text.toString().trim()
        val confirmar   = editConfirmarSenha.text.toString().trim()

        // Validações locais
        if (atual.isBlank()) {
            editSenhaAtual.error = "Informe a senha atual"; return
        }
        if (nova.length < 6) {
            editNovaSenha.error = "Mínimo 6 caracteres"; return
        }
        if (nova != confirmar) {
            editConfirmarSenha.error = "As senhas não coincidem"; return
        }
        if (nova == atual) {
            editNovaSenha.error = "A nova senha deve ser diferente da atual"; return
        }

        val userId = AuthManager.getUserId(this) ?: run {
            Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val body = TrocarSenhaRequest(senhaAtual = atual, novaSenha = nova)

                val resp = if (AuthManager.isMercado(this@TrocarSenhaActivity)) {
                    api.trocarSenhaMercado(userId, body)
                } else {
                    api.trocarSenhaUsuario(userId, body)
                }

                if (resp.isSuccessful) {
                    Toast.makeText(
                        this@TrocarSenhaActivity,
                        "Senha alterada com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val msg = ApiErrorParser.parse(resp)
                    if (msg.contains("senha atual", ignoreCase = true)) {
                        editSenhaAtual.error = "Senha atual incorreta"
                    }
                    Toast.makeText(
                        this@TrocarSenhaActivity,
                        msg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TrocarSenhaActivity,
                    "Sem conexão com o servidor.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSalvar.isEnabled    = !loading
    }
}
