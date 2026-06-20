package com.example.wofertas.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wofertas.Oferta
import com.example.wofertas.data.repository.OfertaRepository
import com.example.wofertas.network.ApiErrorParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para MinhasOfertas.kt (histórico do mercado autenticado).
 *
 * Uso:
 *   viewModel.uiState.collect { ... }
 *   viewModel.deletarOferta(id)
 */
class MinhasOfertasViewModel(private val repository: OfertaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MinhasOfertasUiState>(MinhasOfertasUiState.Loading)
    val uiState: StateFlow<MinhasOfertasUiState> = _uiState.asStateFlow()

    // Evento único de deleção (null = sem evento pendente)
    private val _deletarEvento = MutableSharedFlow<DeletarEvento>()
    val deletarEvento: SharedFlow<DeletarEvento> = _deletarEvento.asSharedFlow()

    init {
        carregarOfertas()
    }

    fun carregarOfertas() {
        viewModelScope.launch {
            _uiState.value = MinhasOfertasUiState.Loading
            repository.fetchMinhasOfertas()
                .onSuccess { lista ->
                    _uiState.value = if (lista.isEmpty())
                        MinhasOfertasUiState.Vazio
                    else
                        MinhasOfertasUiState.Success(lista)
                }
                .onFailure { e ->
                    _uiState.value = MinhasOfertasUiState.Error(
                        ApiErrorParser.fromException(e as Exception)
                    )
                }
        }
    }

    fun deletarOferta(id: String) {
        viewModelScope.launch {
            repository.deleteOferta(id)
                .onSuccess {
                    _deletarEvento.emit(DeletarEvento.Sucesso)
                    carregarOfertas() // recarrega lista
                }
                .onFailure { e ->
                    _deletarEvento.emit(
                        DeletarEvento.Erro(ApiErrorParser.fromException(e as Exception))
                    )
                }
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MinhasOfertasViewModel(OfertaRepository(context)) as T
    }
}

sealed class MinhasOfertasUiState {
    object Loading : MinhasOfertasUiState()
    object Vazio : MinhasOfertasUiState()
    data class Success(val ofertas: List<Oferta>) : MinhasOfertasUiState()
    data class Error(val msg: String) : MinhasOfertasUiState()
}

sealed class DeletarEvento {
    object Sucesso : DeletarEvento()
    data class Erro(val msg: String) : DeletarEvento()
}
