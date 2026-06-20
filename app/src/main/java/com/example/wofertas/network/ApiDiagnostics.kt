package com.example.wofertas.network

import android.content.Context
import com.example.wofertas.utils.AppLogger
import com.example.wofertas.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Ferramenta para diagnosticar e debugar conexão com a API.
 */
object ApiDiagnostics {

    private const val TAG = "ApiDiagnostics"

    /**
     * Teste de conectividade usando a URL ATUAL do ApiClient.
     */
    suspend fun testServerConnection(): DiagnosticResult = withContext(Dispatchers.IO) {
        val currentUrl = ApiClient.getCurrentBaseUrl()
        try {
            AppLogger.info("Iniciando teste de conexão com $currentUrl")

            val url = URL(currentUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET" // Alterado para GET para evitar erro 405/400 em alguns servers

            val responseCode = connection.responseCode
            // Aceita 200-299 (Sucesso) ou 401/403/404 (Servidor respondeu, então há conexão)
            val isConnected = responseCode in 200..499

            AppLogger.info("Status do servidor: $responseCode")
            connection.disconnect()

            if (isConnected) {
                DiagnosticResult.Success("Conectado! O servidor respondeu com status: $responseCode")
            } else {
                DiagnosticResult.Error("Servidor encontrado, mas retornou erro: $responseCode")
            }
        } catch (e: Exception) {
            AppLogger.error("Erro ao conectar ao servidor $currentUrl", e)
            DiagnosticResult.Error("Falha física na conexão: ${e.message}\nVerifique se o IP está correto e o servidor rodando.")
        }
    }

    suspend fun testLogin(email: String, password: String): DiagnosticResult {
        return try {
            val request = LoginRequest(email = email, senha = password)
            val response = ApiClient.publicService.login(request)

            if (response.isSuccessful) {
                DiagnosticResult.Success("Login funciona! User ID: ${response.body()?.id}")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Sem detalhes"
                DiagnosticResult.Error("Erro ${response.code()}: $errorBody")
            }
        } catch (e: Exception) {
            DiagnosticResult.Error("Falha na chamada de login: ${e.message}")
        }
    }

    suspend fun testListOffers(context: Context): DiagnosticResult {
        return try {
            val response = ApiClient.authService(context).listarOfertas()
            if (response.isSuccessful) {
                DiagnosticResult.Success("Listagem OK! Total: ${response.body()?.size} ofertas")
            } else {
                DiagnosticResult.Error("Erro ao listar: ${response.code()}")
            }
        } catch (e: Exception) {
            DiagnosticResult.Error("Erro de rede: ${e.message}")
        }
    }

    fun getDebugInfo(): String {
        return """
            📍 IP Configurado: ${ApiClient.getCurrentBaseUrl()}
            ⏱️ Timeouts: ${Constants.CONNECT_TIMEOUT}s
            🔐 Auth: Bearer Token via Header
            
            DICAS:
            1. Verifique se o backend tem 'server.address=0.0.0.0'
            2. Certifique-se que o celular e PC estão no mesmo Wi-Fi.
        """.trimIndent()
    }

    sealed class DiagnosticResult {
        data class Success(val message: String) : DiagnosticResult()
        data class Error(val message: String) : DiagnosticResult()
        fun toLog(): String = when (this) {
            is Success -> "✅ $message"
            is Error -> "❌ $message"
        }
    }
}
