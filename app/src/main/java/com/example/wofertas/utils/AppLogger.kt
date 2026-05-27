package com.example.wofertas.utils

import android.util.Log

/**
 * Centralized logging utility for the application.
 * Helps with debugging and can be easily configured to enable/disable logs in production.
 */
object AppLogger {

    private const val TAG = "Wofertas"
    private val isDebugBuild = true  // Set to false in production

    /**
     * Log debug messages.
     */
    fun debug(message: String, exception: Throwable? = null) {
        if (isDebugBuild) {
            if (exception != null) {
                Log.d(TAG, message, exception)
            } else {
                Log.d(TAG, message)
            }
        }
    }

    /**
     * Log info messages.
     */
    fun info(message: String) {
        if (isDebugBuild) {
            Log.i(TAG, message)
        }
    }

    /**
     * Log warning messages.
     */
    fun warn(message: String, exception: Throwable? = null) {
        if (isDebugBuild) {
            if (exception != null) {
                Log.w(TAG, message, exception)
            } else {
                Log.w(TAG, message)
            }
        }
    }

    /**
     * Log error messages (always logged, even in production).
     */
    fun error(message: String, exception: Throwable? = null) {
        if (exception != null) {
            Log.e(TAG, message, exception)
        } else {
            Log.e(TAG, message)
        }
    }

    /**
     * Create a tag for a specific class.
     */
    fun tag(clazz: Class<*>): String = clazz.simpleName
}

/**
 * Extension function for easy logging from any class.
 * Usage: logD("Message")
 */
inline fun <reified T> T.logD(message: String) {
    AppLogger.debug("${AppLogger.tag(T::class.java)}: $message")
}

inline fun <reified T> T.logE(message: String, exception: Throwable? = null) {
    AppLogger.error("${AppLogger.tag(T::class.java)}: $message", exception)
}

inline fun <reified T> T.logW(message: String) {
    AppLogger.warn("${AppLogger.tag(T::class.java)}: $message")
}
