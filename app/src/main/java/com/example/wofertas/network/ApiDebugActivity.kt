package com.example.wofertas.network

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.R
import com.example.wofertas.utils.AppLogger
import kotlinx.coroutines.launch

/**
 * Activity de DEBUG para testar conexão com a API Spring Boot.
 *
 * COMO USAR:
 * 1. Adicione a atividade ao AndroidManifest.xml (apenas para debug)
 * 2. Chame desde a MainActivity: startActivity(Intent(this, ApiDebugActivity::class.java))
 * 3. Use os botões para testar os endpoints
 *
 * PARA REMOVER EM PRODUÇÃO:
 * - Delete este arquivo
 * - Remove referência do AndroidManifest.xml
 */
class ApiDebugActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etBaseUrl: EditText
    private lateinit var btnTestConnection: Button
    private lateinit var btnTestLogin: Button
    private lateinit var btnTestOffers: Button
    private lateinit var btnChangeEnvironment: Button
    private lateinit var btnClearLogs: Button
    private lateinit var scrollView: ScrollView

    private val logs = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_debug)

        initViews()
        setupListeners()
        printDebugInfo()
    }

    private fun initViews() {
        tvOutput = findViewById(R.id.tvDebugOutput)
        etEmail = findViewById(R.id.etDebugEmail)
        etPassword = findViewById(R.id.etDebugPassword)
        etBaseUrl = findViewById(R.id.etDebugBaseUrl)
        btnTestConnection = findViewById(R.id.btnTestConnection)
        btnTestLogin = findViewById(R.id.btnTestLogin)
        btnTestOffers = findViewById(R.id.btnTestOffers)
        btnChangeEnvironment = findViewById(R.id.btnChangeEnvironment)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        scrollView = findViewById(R.id.scrollViewDebug)

        etEmail.setText("teste@wofertas.com")
        etPassword.setText("senha123")
        etBaseUrl.setText(ApiClient.getCurrentBaseUrl())
    }

    private fun setupListeners() {
        btnTestConnection.setOnClickListener {
            lifecycleScope.launch {
                addLog("▶ Iniciando teste de conexão...")
                val result = ApiDiagnostics.testServerConnection()
                addLog(result.toLog())
            }
        }

        btnTestLogin.setOnClickListener {
            lifecycleScope.launch {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    addLog("❌ Email e senha são obrigatórios")
                    return@launch
                }

                addLog("▶ Testando login com: $email")
                val result = ApiDiagnostics.testLogin(email, password)
                addLog(result.toLog())
            }
        }

        btnTestOffers.setOnClickListener {
            lifecycleScope.launch {
                addLog("▶ Testando listagem de ofertas...")
                val result = ApiDiagnostics.testListOffers(this@ApiDebugActivity)
                addLog(result.toLog())
            }
        }

        btnChangeEnvironment.setOnClickListener {
            val newUrl = etBaseUrl.text.toString().trim()
            if (newUrl.isEmpty()) {
                addLog("❌ URL não pode estar vazia")
                return@setOnClickListener
            }

            try {
                // CORREÇÃO: Usar a nova URL/IP digitado em vez de resetar para o padrão
                ApiClient.updateBaseUrl(newUrl)
                addLog("✅ Configurado para nova URL: ${ApiClient.getCurrentBaseUrl()}")
                addLog("⚠️ Nota: O App agora tentará conectar neste endereço.")
            } catch (e: Exception) {
                addLog("❌ Erro ao mudar URL: ${e.message}")
            }
        }

        btnClearLogs.setOnClickListener {
            logs.clear()
            tvOutput.text = ""
            addLog("🧹 Logs limpos")
        }
    }

    private fun printDebugInfo() {
        addLog("╔════════════════════════════════════════════════════╗")
        addLog("║         WOFERTAS API DEBUG ACTIVITY                ║")
        addLog("╚════════════════════════════════════════════════════╝")
        addLog("")
        addLog(ApiDiagnostics.getDebugInfo())
        addLog("")
        addLog("📍 Base URL Atual: ${ApiClient.getCurrentBaseUrl()}")
        addLog("")
        addLog("✅ DEBUG TOOLS PRONTOS")
        addLog("")
    }

    private fun addLog(message: String) {
        logs.add(message)
        tvOutput.text = logs.joinToString("\n")

        // Scroll automático para o final
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }

        // Também log no Android Studio
        AppLogger.debug(message)
    }
}
