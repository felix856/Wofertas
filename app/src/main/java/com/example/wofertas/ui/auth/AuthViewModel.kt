package com.example.wofertas.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.LoginRequest
import com.example.wofertas.ui.base.BaseViewModel
import com.example.wofertas.utils.AppLogger
import com.example.wofertas.utils.NetworkUtils
import com.example.wofertas.utils.Result
import com.example.wofertas.utils.ValidationUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication operations (login, signup, logout).
 * Handles business logic and data operations, separating concerns from the Activity.
 */
class AuthViewModel : BaseViewModel() {

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _signupSuccess = MutableLiveData<Boolean>()
    val signupSuccess: LiveData<Boolean> = _signupSuccess

    /**
     * Perform login with email and password.
     */
    fun login(context: Context, email: String, password: String) {
        // Validate inputs
        val emailError = ValidationUtils.getValidationError("email", email)
        if (emailError != null) {
            setError(emailError)
            return
        }

        val passwordError = ValidationUtils.getValidationError("senha", password)
        if (passwordError != null) {
            setError(passwordError)
            return
        }

        // Check network
        if (!NetworkUtils.isNetworkAvailable(context)) {
            setError("Sem conexão de internet. Verifique sua conexão.")
            return
        }

        // Perform login
        setLoading(true)
        viewModelScope.launch {
            try {
                val request = LoginRequest(email = email, senha = password)
                val response = ApiClient.publicService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // Save session
                    com.example.wofertas.AuthManager.saveSession(
                        context = context,
                        token = loginResponse.token,
                        userId = loginResponse.id,
                        email = loginResponse.email,
                        tipo = loginResponse.tipo
                    )

                    AppLogger.debug("Login successful for user: ${loginResponse.id}")
                    _loginSuccess.value = true
                    clearError()
                } else {
                    val errorMsg = "Falha no login: ${response.code()}"
                    AppLogger.warn(errorMsg)
                    setError(errorMsg)
                }
            } catch (e: Exception) {
                val result = NetworkUtils.handleHttpError(e)
                setError(result.message)
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Clear success flag after navigation.
     */
    fun clearLoginSuccess() {
        _loginSuccess.value = false
    }

    fun clearSignupSuccess() {
        _signupSuccess.value = false
    }
}
