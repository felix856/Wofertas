package com.example.wofertas

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.ApiErrorParser
import com.example.wofertas.network.CadastroMercadoRequest
import com.example.wofertas.network.CadastroUsuarioRequest
import com.example.wofertas.network.LoginRequest
import kotlinx.coroutines.launch

/**
 * Tela de Cadastro Corrigida: Envia telefone para evitar Erro 400 no Backend.
 */
class Cadastro : AppCompatActivity() {

    private lateinit var edtNome: EditText
    private lateinit var edtCNPJ: EditText
    private lateinit var edtTelefone: EditText
    private lateinit var edtEndereco: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var edtConfirmaSenha: EditText
    private lateinit var rgPerfil: RadioGroup
    private lateinit var rbCliente: RadioButton
    private lateinit var rbSupermercado: RadioButton
    private lateinit var btnContinuar: Button
    private lateinit var btnVoltar: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        rgPerfil        = findViewById(R.id.rgPerfil)
        rbCliente       = findViewById(R.id.rbCliente)
        rbSupermercado  = findViewById(R.id.rbSupermercado)
        edtNome         = findViewById(R.id.edtNome)
        edtCNPJ         = findViewById(R.id.edtCNPJ)
        edtTelefone     = findViewById(R.id.edtTelefone)
        edtEndereco     = findViewById(R.id.edtEndereco)
        edtEmail        = findViewById(R.id.edtEmail)
        edtSenha        = findViewById(R.id.edtSenha)
        edtConfirmaSenha = findViewById(R.id.edtConfirmaSenha)
        btnContinuar    = findViewById(R.id.btnContinuar)
        btnVoltar       = findViewById(R.id.btnVoltar)
        progressBar     = findViewById(R.id.progressBarCadastro)

        atualizarCamposPorPerfil()
        rgPerfil.setOnCheckedChangeListener { _, _ -> atualizarCamposPorPerfil() }
        btnContinuar.setOnClickListener { validarECadastrar() }
        btnVoltar.setOnClickListener    { finish() }
    }

    private fun validarECadastrar() {
        val nome       = edtNome.text.toString().trim()
        val cnpj       = edtCNPJ.text.toString().trim()
        val telefone   = edtTelefone.text.toString().trim()
        val endereco   = edtEndereco.text.toString().trim()
        val email      = edtEmail.text.toString().trim()
        val senha      = edtSenha.text.toString().trim()
        val confirma   = edtConfirmaSenha.text.toString().trim()
        val isMercado  = rbSupermercado.isChecked

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirma.isEmpty()) {
            toast("Preencha todos os campos obrigatórios."); return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "E-mail inválido."; return
        }
        if (senha.length < 6) {
            edtSenha.error = "Mínimo 6 caracteres."; return
        }
        if (senha != confirma) {
            edtConfirmaSenha.error = "As senhas não coincidem."; return
        }
        
        if (isMercado) {
            if (cnpj.length < 14) {
                edtCNPJ.error = "CNPJ inválido."; return
            }
            if (telefone.isEmpty()) {
                edtTelefone.error = "Telefone é obrigatório."; return
            }
            if (endereco.isEmpty()) {
                edtEndereco.error = "Endereço obrigatório."; return
            }
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                if (isMercado) {
                    cadastrarMercado(nome, cnpj, telefone, endereco, email, senha)
                } else {
                    cadastrarUsuario(nome, email, senha)
                }
            } catch (e: Exception) {
                toast(ApiErrorParser.fromException(e))
                setLoading(false)
            }
        }
    }

    private suspend fun cadastrarUsuario(nome: String, email: String, senha: String) {
        val resp = ApiClient.publicService.cadastrarUsuario(
            CadastroUsuarioRequest(nome = nome, email = email, senha = senha)
        )
        if (resp.isSuccessful) {
            autoLoginERediredir(email, senha)
        } else {
            val msg = mensagemCadastroUsuario(resp.code(), ApiErrorParser.parse(resp))
            toast(msg)
            setLoading(false)
        }
    }

    private suspend fun cadastrarMercado(
        nome: String, cnpj: String, telefone: String, endereco: String, email: String, senha: String
    ) {
        // FIX: Agora enviamos o telefone corretamente para a API
        val resp = ApiClient.publicService.cadastrarMercado(
            CadastroMercadoRequest(
                nome     = nome,
                cnpj     = cnpj,
                telefone = telefone, // Adicionado telefone
                endereco = endereco,
                email    = email,
                senha    = senha
            )
        )
        if (resp.isSuccessful) {
            autoLoginERediredir(email, senha)
        } else {
            val msg = mensagemCadastroMercado(resp.code(), ApiErrorParser.parse(resp))
            toast(msg)
            setLoading(false)
        }
    }

    private fun mensagemCadastroUsuario(code: Int, apiMessage: String): String {
        val normalized = apiMessage.lowercase()
        return when {
            code == 409 || normalized.contains("ja cadastrado") || normalized.contains("já cadastrado") ->
                "Este e-mail ja esta cadastrado. Tente entrar ou recuperar a senha."
            apiMessage.isNotBlank() && !apiMessage.startsWith("Erro $code") ->
                apiMessage
            else ->
                "Nao foi possivel criar o cadastro. Verifique os dados e tente novamente."
        }
    }

    private fun mensagemCadastroMercado(code: Int, apiMessage: String): String {
        val normalized = apiMessage.lowercase()
        return when {
            code == 409 || normalized.contains("ja cadastrado") || normalized.contains("já cadastrado") ->
                "CNPJ ou e-mail ja cadastrado. Tente entrar ou recuperar a senha."
            apiMessage.isNotBlank() && !apiMessage.startsWith("Erro $code") ->
                apiMessage
            else ->
                "Nao foi possivel cadastrar o mercado. Verifique CNPJ, telefone e endereco."
        }
    }

    private suspend fun autoLoginERediredir(email: String, senha: String) {
        try {
            val loginResp = ApiClient.publicService.login(LoginRequest(email, senha))
            if (loginResp.isSuccessful) {
                val body = loginResp.body()
                if (body == null || body.token.isBlank() || body.id.isBlank() || body.tipo.isBlank()) {
                    toast("Cadastro realizado. Faca login para continuar.")
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    return
                }
                AuthManager.saveSession(this, body.token, body.id, body.email, body.tipo, body.nome)
                toast("Cadastro realizado com sucesso!")
                val destino = when {
                    AuthManager.isMercado(this) -> DashboardSupermercadoActivity::class.java
                    AuthManager.isUsuario(this) -> ListaOfertas::class.java
                    else -> {
                        AuthManager.clearSession(this)
                        LoginActivity::class.java
                    }
                }
                startActivity(Intent(this, destino).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                finish()
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun atualizarCamposPorPerfil() {
        val visibility = if (rbSupermercado.isChecked) View.VISIBLE else View.GONE
        edtNome.hint = if (rbSupermercado.isChecked) "Nome da Loja" else "Nome Completo"
        
        edtCNPJ.visibility = visibility
        edtTelefone.visibility = visibility // Garante que o telefone apareça para Mercado
        edtEndereco.visibility = visibility
        
        findViewById<View>(R.id.labelCNPJ)?.visibility = visibility
        findViewById<View>(R.id.labelEndereco)?.visibility = visibility
        findViewById<View>(R.id.labelTelefone)?.visibility = visibility
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnContinuar.isEnabled = !isLoading
        btnVoltar.isEnabled    = !isLoading
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
