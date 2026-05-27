// app/src/main/java/com/example/wofertas/LoginActivity.kt
package com.example.wofertas

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.ApiDebugActivity
import com.example.wofertas.network.LoginRequest
import kotlinx.coroutines.launch

/**
 * Tela de login — sem Firebase.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var btnEntrar: Button
    private lateinit var btnCadastrar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnVoltar: Button
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (AuthManager.isLoggedIn(this)) {
            redirecionarPorTipo()
            return
        }
        findViewById<View>(R.id.llHeaderLogin).setOnClickListener {
            startActivity(Intent(this, ApiDebugActivity::class.java))
        }
        edtEmail    = findViewById(R.id.email)
        edtSenha    = findViewById(R.id.senha)
        btnEntrar   = findViewById(R.id.btn_entrar)
        btnCadastrar = findViewById(R.id.button3)
        progressBar = findViewById(R.id.progressBarLogin)
        btnVoltar = findViewById(R.id.btn_voltar)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        btnEntrar.setOnClickListener   { validarEFazerLogin() }
        btnCadastrar.setOnClickListener { startActivity(Intent(this, Cadastro::class.java)) }
        btnVoltar.setOnClickListener {
            finish()
        }
        tvForgotPassword.setOnClickListener {
            mostrarDialogRecuperarSenha()
        }

        configurarToggleSenha()
    }

    private fun mostrarDialogRecuperarSenha() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recuperar Senha")
        builder.setMessage("Informe seu e-mail para receber o código de 6 dígitos.")

        val input = EditText(this)
        input.hint = "seu@email.com.br"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = android.widget.FrameLayout(this)
        container.addView(input)
        input.setPadding(padding, padding / 2, padding, padding / 2)
        builder.setView(container)

        builder.setPositiveButton("Enviar Código") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                solicitarRecuperacaoSenha(email)
            } else {
                Toast.makeText(this, "Informe um e-mail válido.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun solicitarRecuperacaoSenha(email: String) {
        setCarregando(true)
        lifecycleScope.launch {
            try {
                val response = ApiClient.publicService.forgotPassword(email)
                if (response.isSuccessful) {
                    Toast.makeText(this@LoginActivity, "Código enviado para o e-mail!", Toast.LENGTH_SHORT).show()
                    // Abre a tela de redefinição passando o e-mail
                    val intent = Intent(this@LoginActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@LoginActivity, "Erro ao enviar código. Verifique o e-mail.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Erro de conexão.", Toast.LENGTH_LONG).show()
            } finally {
                setCarregando(false)
            }
        }
    }

    private fun validarEFazerLogin() {
        val email = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString().trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Informe um e-mail válido."
            edtEmail.requestFocus()
            return
        }
        if (senha.length < 6) {
            edtSenha.error = "A senha deve ter pelo menos 6 caracteres."
            edtSenha.requestFocus()
            return
        }

        fazerLogin(email, senha)
    }

    private fun fazerLogin(email: String, senha: String) {
        setCarregando(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.publicService.login(LoginRequest(email, senha))

                if (response.isSuccessful) {
                    val body = response.body()!!
                    AuthManager.saveSession(
                        context = this@LoginActivity,
                        token   = body.token,
                        userId  = body.id,
                        email   = body.email,
                        tipo    = body.tipo
                    )
                    Toast.makeText(this@LoginActivity, "Login efetuado com sucesso!", Toast.LENGTH_SHORT).show()
                    redirecionarPorTipo()
                } else {
                    val msg = when (response.code()) {
                        401  -> "E-mail ou senha incorretos."
                        403  -> "Acesso negado."
                        404  -> "Usuário não encontrado."
                        else -> "Erro ao fazer login (${response.code()}). Tente novamente."
                    }
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                    setCarregando(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show()
                setCarregando(false)
            }
        }
    }

    private fun redirecionarPorTipo() {
        val destino = if (AuthManager.isMercado(this)) {
            DashboardSupermercadoActivity::class.java
        } else {
            ListaOfertas::class.java
        }
        startActivity(
            Intent(this, destino).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        finish()
    }

    private fun setCarregando(carregando: Boolean) {
        progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
        btnEntrar.isEnabled    = !carregando
        btnCadastrar.isEnabled = !carregando
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarToggleSenha() {
        edtSenha.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_off_24, 0)
        edtSenha.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawable = edtSenha.compoundDrawables[2]
                if (drawable != null && event.rawX >= edtSenha.right - drawable.bounds.width()) {
                    val cursor = edtSenha.selectionEnd
                    val isVisible = edtSenha.inputType ==
                            (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                    edtSenha.inputType = if (isVisible) {
                        android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    } else {
                        android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    }
                    edtSenha.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0,
                        if (isVisible) R.drawable.baseline_visibility_off_24 else R.drawable.baseline_visibility_24,
                        0
                    )
                    edtSenha.setSelection(cursor)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}