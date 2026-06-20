package com.example.wofertas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wofertas.data.repository.MercadoRepository
import com.example.wofertas.network.*
import kotlinx.coroutines.launch

class PerfilViewModel(
    private val mercadoRepo: MercadoRepository
) : ViewModel() {

    private val _perfilMercado = MutableLiveData<MercadoResponse>()
    val perfilMercado: LiveData<MercadoResponse> = _perfilMercado

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun carregarPerfilMercado() {
        viewModelScope.launch {
            _loading.value = true
            when (val result = mercadoRepo.getMercadoPerfil()) {
                is ApiResult.Success -> _perfilMercado.value = result.data
                is ApiResult.Error -> _error.value = result.exception.message
            }
            _loading.value = false
        }
    }

    fun atualizarPerfilMercado(nome: String, endereco: String) {
        // Implementar lógica de atualização no Repository se necessário
    }
}
