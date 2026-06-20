package com.example.wofertas.utils

import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Extension functions for common UI operations.
 */

/**
 * Show a short toast message.
 */
fun AppCompatActivity.showShortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Show a long toast message.
 */
fun AppCompatActivity.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * Get trimmed text from EditText.
 */
fun EditText.getTrimmedText(): String = this.text.toString().trim()

/**
 * Clear error when user starts typing.
 */
fun EditText.clearErrorOnEdit() {
    this.setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
            this.error = null
        }
    }
}

/**
 * Set error and request focus.
 */
fun EditText.setErrorAndFocus(errorMessage: String) {
    this.error = errorMessage
    this.requestFocus()
}
