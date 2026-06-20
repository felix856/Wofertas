package com.example.wofertas.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.wofertas.utils.Result

/**
 * Base ViewModel class for common functionality across all ViewModels.
 * Provides state management and error handling patterns.
 */
abstract class BaseViewModel : ViewModel() {

    // Loading state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Success message state
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    protected fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    protected fun setError(message: String?) {
        _error.value = message
    }

    protected fun setSuccessMessage(message: String?) {
        _successMessage.value = message
    }

    protected fun clearError() {
        _error.value = null
    }

    protected fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Handle a Result object and update the UI state accordingly.
     */
    protected fun <T> handleResult(
        result: Result<T>,
        onSuccess: (T) -> Unit = {}
    ) {
        when (result) {
            is Result.Success -> {
                setLoading(false)
                clearError()
                onSuccess(result.data)
            }
            is Result.Error -> {
                setLoading(false)
                setError(result.message)
            }
            is Result.Loading -> {
                setLoading(true)
                clearError()
            }
        }
    }
}
