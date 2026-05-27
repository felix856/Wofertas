package com.example.wofertas.utils

import android.os.Build

/**
 * Centralized constants for the application.
 */
object Constants {

    /**
     * URL Automática:
     * 1. No Emulador: Usa 10.0.2.2 (que é o localhost do PC).
     * 2. No Celular Real: Usa localhost (requer o comando 'adb reverse' via cabo ou wifi).
     */
    val BASE_URL: String
        get() = if (isEmulator()) {
            "http://10.0.2.2:8080/"
        } else {
            "http://192.168.3.201:8080/"
        }

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

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
