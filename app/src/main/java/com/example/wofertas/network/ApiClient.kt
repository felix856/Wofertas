package com.example.wofertas.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.wofertas.AuthManager
import com.example.wofertas.LoginActivity
import com.example.wofertas.utils.AppLogger
import com.example.wofertas.utils.Constants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Provedor centralizado do Retrofit com tratamento de rede e segurança.
 */
object ApiClient {

    private var currentBaseUrl: String = Constants.BASE_URL

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    private var cachedToken: String? = null
    private var cachedAuthService: ApiService? = null
    private var cachedBaseUrl: String? = null

    /**
     * SERVIÇO PÚBLICO: Login, Cadastro e Recuperação de Senha.
     */
    val publicService: ApiService
        get() {
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService::class.java)
        }

    /**
     * SERVIÇO AUTENTICADO: Exige JWT e lida com erro 401 automaticamente.
     */
    fun authService(context: Context): ApiService {
        val token = AuthManager.getToken(context)

        if (token != cachedToken || cachedAuthService == null || currentBaseUrl != cachedBaseUrl) {
            cachedToken = token
            cachedBaseUrl = currentBaseUrl

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logger)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val requestBuilder = request.newBuilder()
                        .header("Accept", "application/json")
                        .header("User-Agent", "Wofertas-Android-App")

                    if (token != null) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }

                    val response = try {
                        chain.proceed(requestBuilder.build())
                    } catch (e: Exception) {
                        // Lança uma exceção mais amigável para o log
                        throw IOException("Falha ao conectar ao servidor em $currentBaseUrl. Verifique o IP e se o backend está rodando.")
                    }

                    if (response.code == 401) {
                        AppLogger.error("Sessão expirada (401). Limpando dados e redirecionando...")
                        AuthManager.clearSession(context)
                        val intent = Intent(context, LoginActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        context.startActivity(intent)
                    }
                    response
                }
                .retryOnConnectionFailure(true)
                .build()

            cachedAuthService = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService::class.java)
        }

        return cachedAuthService!!
    }

    fun getCurrentBaseUrl(): String = currentBaseUrl

    fun updateBaseUrl(newIp: String) {
        val formattedUrl = when {
            newIp.startsWith("http") -> if (newIp.endsWith("/")) newIp else "$newIp/"
            else -> "http://$newIp:8080/"
        }
        currentBaseUrl = formattedUrl
        invalidateAuthService()
    }

    fun invalidateAuthService() {
        cachedToken = null
        cachedAuthService = null
        cachedBaseUrl = null
    }
}
