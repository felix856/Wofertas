package com.example.wofertas.utils

/**
 * A wrapper class for handling API responses uniformly.
 * Separates success from failure cases without throwing exceptions.
 *
 * Usage:
 *   when (result) {
 *       is Result.Success -> handleData(result.data)
 *       is Result.Error   -> handleError(result.message, result.exception)
 *       is Result.Loading -> showLoadingUI()
 *   }
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()

    /**
     * Maps the success value using the provided function.
     * Returns the same Error or Loading state if not Success.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(this.data))
        is Error -> this
        is Loading -> Loading
    }

    /**
     * Executes a block if the result is Success.
     */
    inline fun onSuccess(block: (T) -> Unit): Result<T> = apply {
        if (this is Success) block(this.data)
    }

    /**
     * Executes a block if the result is Error.
     */
    inline fun onError(block: (String, Exception?) -> Unit): Result<T> = apply {
        if (this is Error) block(this.message, this.exception)
    }

    /**
     * Get the data if it's a Success, otherwise null.
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Check if result is success.
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Check if result is error.
     */
    fun isError(): Boolean = this is Error
}
