package com.example.wofertas.utils

/**
 * Safe extension functions for null-safe operations.
 */

/**
 * Execute a block if the string is not null or empty.
 */
inline fun String?.ifNotEmpty(block: (String) -> Unit) {
    if (!this.isNullOrEmpty()) {
        block(this)
    }
}

/**
 * Get string value or default if null/empty.
 */
fun String?.orEmpty(default: String = ""): String {
    return if (this.isNullOrEmpty()) default else this
}

/**
 * Truncate string to max length.
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) {
        this.substring(0, maxLength) + "..."
    } else {
        this
    }
}

/**
 * Check if string is a valid number.
 */
fun String.isNumeric(): Boolean {
    return this.toDoubleOrNull() != null
}

/**
 * Execute a block if the value is not null.
 */
inline fun <T> T?.ifNotNull(block: (T) -> Unit) {
    if (this != null) {
        block(this)
    }
}

/**
 * Safe list filtering.
 */
fun <T> List<T>?.orEmpty(): List<T> {
    return this ?: emptyList()
}
