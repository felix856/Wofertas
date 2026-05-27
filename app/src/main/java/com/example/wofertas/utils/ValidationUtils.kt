package com.example.wofertas.utils

import android.util.Patterns

/**
 * Utility functions for input validation.
 */
object ValidationUtils {

    /**
     * Validate if string is a valid email.
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate if password meets minimum requirements.
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    /**
     * Validate if name meets minimum requirements.
     */
    fun isValidName(name: String): Boolean {
        return name.trim().length >= Constants.MIN_NAME_LENGTH
    }

    /**
     * Validate if CNPJ format is valid (basic check).
     */
    fun isValidCNPJ(cnpj: String): Boolean {
        val cleanCnpj = cnpj.replace(Regex("[^0-9]"), "")
        return cleanCnpj.length == 14
    }

    /**
     * Check if a string is not empty.
     */
    fun isNotEmpty(text: String): Boolean {
        return text.isNotEmpty()
    }

    /**
     * Check if a string is null or empty.
     */
    fun isEmpty(text: String?): Boolean {
        return text.isNullOrEmpty()
    }

    /**
     * Get the most appropriate error message for a field.
     */
    fun getValidationError(field: String, value: String): String? = when {
        field.equals("email", ignoreCase = true) && !isValidEmail(value) ->
            Constants.ERROR_INVALID_EMAIL
        field.equals("senha", ignoreCase = true) && !isValidPassword(value) ->
            Constants.ERROR_PASSWORD_TOO_SHORT
        field.equals("nome", ignoreCase = true) && !isValidName(value) ->
            "Nome deve ter no mínimo ${Constants.MIN_NAME_LENGTH} caracteres."
        field.equals("cnpj", ignoreCase = true) && !isValidCNPJ(value) ->
            "CNPJ inválido."
        isEmpty(value) ->
            Constants.ERROR_EMPTY_FIELD
        else -> null
    }
}
