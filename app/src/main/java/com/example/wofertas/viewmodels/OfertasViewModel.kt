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
 * ViewModel para a tela de lista de ofertas (ListaOfertas.kt).
 *
 * Estados expostos via StateFlow:
 *   uiState  → carregando / dados / erro
 *   query    → texto de busca atual
 *
 * Uso na Activity:
 *   lifecycleScope.launch {
 *       viewModel.uiState.collect { state ->
 *           when (state) {
 *               is OfertasUiState.Loading -> showProgress()
 *               is OfertasUiState.Success -> adapter.atualizarListaCompleta(state.ofertas)
 *               is OfertasUiState.Error   -> showError(state.msg)
 *           }
 *       }
 *   }
 */
class OfertasViewModel(private val repository: OfertaRepository) : ViewModel() {

    // ── Estado principal ──────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<OfertasUiState>(OfertasUiState.Loading)
    val uiState: StateFlow<OfertasUiState> = _uiState.asStateFlow()

    // Lista completa (sem filtro) para busca local
    private var listaCompleta: List<Oferta> = emptyList()

    // ── Busca ─────────────────────────────────────────────────────────────────
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        carregarOfertas()
    }

    fun carregarOfertas() {
        viewModelScope.launch {
            _uiState.value = OfertasUiState.Loading
            repository.fetchOfertas()
                .onSuccess { lista ->
                    listaCompleta = lista
                    _uiState.value = OfertasUiState.Success(filtrar(lista, _query.value))
                }
                .onFailure { e ->
                    _uiState.value = OfertasUiState.Error(
                        ApiErrorParser.fromException(e as Exception)
                    )
                }
        }
    }

    fun buscar(query: String) {
        _query.value = query
        val estado = _uiState.value
        if (estado is OfertasUiState.Success || listaCompleta.isNotEmpty()) {
            _uiState.value = OfertasUiState.Success(filtrar(listaCompleta, query))
        }
    }

    private fun filtrar(lista: List<Oferta>, q: String): List<Oferta> {
        if (q.isBlank()) return lista
        val lower = q.lowercase()
        return lista.filter { oferta ->
            oferta.nome?.lowercase()?.contains(lower) == true ||
            oferta.nomeSupermercado?.lowercase()?.contains(lower) == true ||
            oferta.enderecoSupermercado?.lowercase()?.contains(lower) == true
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OfertasViewModel(OfertaRepository(context)) as T
    }
}

sealed class OfertasUiState {
    object Loading : OfertasUiState()
    data class Success(val ofertas: List<Oferta>) : OfertasUiState()
    data class Error(val msg: String) : OfertasUiState()
}
