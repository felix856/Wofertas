package com.example.wofertas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.ResetPasswordRequest
import com.example.wofertas.utils.ValidationUtils
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var edtCode: EditText
    private lateinit var edtNewPass: EditText
    private lateinit var edtConfirmPass: EditText
    private lateinit var btnReset: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        userEmail = intent.getStringExtra("email") ?: ""
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Erro ao recuperar e-mail.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar_reset_password)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        edtCode = findViewById(R.id.edtResetCode)
        edtNewPass = findViewById(R.id.edtNewPassword)
        edtConfirmPass = findViewById(R.id.edtConfirmNewPassword)
        btnReset = findViewById(R.id.btnResetPassword)
        progressBar = findViewById(R.id.progressBarReset)

        btnReset.setOnClickListener { validarERedefinir() }
    }

    private fun validarERedefinir() {
        val code = edtCode.text.toString().trim()
        val pass = edtNewPass.text.toString().trim()
        val confirm = edtConfirmPass.text.toString().trim()

        if (code.length < 6) {
            edtCode.error = "Digite o código de 6 dígitos."
            return
        }
        if (!ValidationUtils.isValidPassword(pass)) {
            edtNewPass.error = "A senha deve ter pelo menos 6 caracteres."
            return
        }
        if (pass != confirm) {
            edtConfirmPass.error = "As senhas não coincidem."
            return
        }

        redefinirSenha(code, pass)
    }

    private fun redefinirSenha(token: String, novaSenha: String) {
        setCarregando(true)
        lifecycleScope.launch {
            try {
                val request = ResetPasswordRequest(userEmail, token, novaSenha)
                val response = ApiClient.publicService.resetPassword(request)

                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, "Senha alterada com sucesso!", Toast.LENGTH_LONG).show()
                    // Volta para o Login
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@ResetPasswordActivity, "Código inválido ou expirado.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ResetPasswordActivity, "Erro de conexão.", Toast.LENGTH_SHORT).show()
            } finally {
                setCarregando(false)
            }
        }
    }

    private fun setCarregando(carregando: Boolean) {
        progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
        btnReset.isEnabled = !carregando
    }
}
