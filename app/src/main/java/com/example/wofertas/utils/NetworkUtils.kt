package com.example.wofertas.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Utility functions for network-related operations.
 */
object NetworkUtils {

    /**
     * Check if the device is connected to the internet.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Convert an exception into a user-friendly error message.
     */
    fun getErrorMessage(exception: Exception): String = when (exception) {
        is SocketTimeoutException -> Constants.ERROR_TIMEOUT
        is IOException -> Constants.ERROR_NETWORK
        is HttpException -> {
            when (exception.code()) {
                400 -> "Requisição inválida."
                401 -> "Não autorizado. Faça login novamente."
                403 -> "Acesso negado."
                404 -> "Recurso não encontrado."
                500 -> "Erro no servidor. Tente novamente mais tarde."
                else -> "Erro HTTP: ${exception.code()}"
            }
        }
        else -> Constants.ERROR_GENERIC
    }

    /**
     * Safely handle Retrofit/HTTP errors.
     */
    fun handleHttpError(exception: Exception): Result.Error {
        val message = getErrorMessage(exception)
        AppLogger.error("HTTP Error: $message", exception)
        return Result.Error(message, exception)
    }
}
