package com.example.wofertas.utils

import android.os.Build

/**
 * Centralized constants for the application.
 */
object Constants {

    /**
     * URL Automática:
     * Conectando diretamente na nuvem (Railway) para todos os dispositivos.
     */
    val BASE_URL: String
        get() = "https://wofertas-production.up.railway.app/"

    const val PRIVACY_POLICY_URL = "https://wofertas.vercel.app/privacy-policy.html"
    const val ACCOUNT_DELETION_URL = "https://wofertas.vercel.app/excluir-conta.html"

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    const val PREFS_NAME = "wofertas_session"
    const val KEY_TOKEN = "token"
    const val KEY_USER_ID = "userId"
    const val KEY_EMAIL = "email"
    const val KEY_TIPO = "tipo"

    const val TIPO_USUARIO = "USUARIO"
    const val TIPO_MERCADO = "MERCADO"

    // Validation Constants
    const val MIN_NAME_LENGTH = 3
    const val MIN_PASSWORD_LENGTH = 6

    // Error Messages
    const val ERROR_NETWORK = "Erro de conexão. Verifique sua internet."
    const val ERROR_TIMEOUT = "Requisição expirou. Tente novamente."
    const val ERROR_GENERIC = "Algo deu errado. Tente novamente."
    const val ERROR_INVALID_EMAIL = "Informe um e-mail válido."
    const val ERROR_PASSWORD_TOO_SHORT = "A senha deve ter pelo menos 6 caracteres."
    const val ERROR_EMPTY_FIELD = "Este campo não pode estar vazio."
}
