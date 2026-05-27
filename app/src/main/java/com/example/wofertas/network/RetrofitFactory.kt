package com.example.wofertas.network

import android.content.Context
import com.example.wofertas.utils.AppLogger
import com.example.wofertas.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory para criar instâncias do Retrofit com diferentes configurações.
 * Útil para testar com diferentes ambientes ou interceptadores.
 */
object RetrofitFactory {

    /**
     * Criar Retrofit com configuração personalizada.
     * Use quando precisar de diferentes configurações para testes.
     */
    fun createService(
        baseUrl: String = Constants.BASE_URL,
        isDebug: Boolean = true,
        timeout: Long = Constants.CONNECT_TIMEOUT
    ): ApiService {
        val logger = HttpLoggingInterceptor().apply {
            level = if (isDebug) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        val client = OkHttpClient.Builder()
            .apply {
                if (isDebug) {
                    addInterceptor(logger)
                    AppLogger.debug("Retrofit com FULL LOGGING habilitado")
                }
            }
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Criar serviço com autenticação (Bearer token).
     */
    fun createAuthenticatedService(
        token: String,
        baseUrl: String = Constants.BASE_URL,
        isDebug: Boolean = true,
        timeout: Long = Constants.CONNECT_TIMEOUT
    ): ApiService {
        val logger = HttpLoggingInterceptor().apply {
            level = if (isDebug) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()
                    .let { chain.proceed(it) }
            }
            .apply {
                if (isDebug) {
                    addInterceptor(logger)
                }
            }
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
