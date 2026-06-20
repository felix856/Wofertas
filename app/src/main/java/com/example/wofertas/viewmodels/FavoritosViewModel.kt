package com.example.wofertas.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wofertas.AuthManager
import com.example.wofertas.data.local.entities.FavoritoEntity
import com.example.wofertas.data.repository.FavoritoRepository
import com.example.wofertas.network.MercadoDto
import com.example.wofertas.network.ApiErrorParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar favoritos do usuário.
 *
 * Uso:
 *   viewModel.uiState.collect { state -> ... }
 *   viewModel.toggleFavorito(mercadoId, mercadoDto)
 *   viewModel.isFavorito(mercadoId)  → Boolean via StateFlow
 *
 * NOTA: As Activities/Fragments existentes que verificam favoritos
 * (ex: OfertaAdapter, ListaOfertas) podem usar este ViewModel via
 * viewModels<FavoritosViewModel> { FavoritosViewModel.Factory(this) }
 */
class FavoritosViewModel(
    private val context: Context,
    private val repository: FavoritoRepository
) : ViewModel() {

    private val usuarioId: String?
        get() = AuthManager.getUserId(context)

    // ── Estado lista de favoritos ─────────────────────────────────────────────
    private val _uiState = MutableStateFlow<FavoritosUiState>(FavoritosUiState.Loading)
    val uiState: StateFlow<FavoritosUiState> = _uiState.asStateFlow()

    // Set de IDs dos mercados favoritados (para verificação rápida nos adapters)
    private val _favoritoIds = MutableStateFlow<Set<String>>(emptySet())
    val favoritoIds: StateFlow<Set<String>> = _favoritoIds.asStateFlow()

    // Evento único de toggle (sucesso/erro)
    private val _toggleEvento = MutableSharedFlow<ToggleEvento>()
    val toggleEvento: SharedFlow<ToggleEvento> = _toggleEvento.asSharedFlow()

    init {
        val uid = usuarioId
        if (!uid.isNullOrEmpty()) {
            // Observa cache local em tempo real
            viewModelScope.launch {
                repository.getFavoritosFlow(uid).collect { lista ->
                    _favoritoIds.value = lista.map { it.mercadoId }.toSet()
                    _uiState.value = if (lista.isEmpty())
                        FavoritosUiState.Vazio
                    else
                        FavoritosUiState.Success(lista)
                }
            }
            // Sincroniza com servidor
            fetchFavoritos()
        } else {
            _uiState.value = FavoritosUiState.Vazio
        }
    }

    fun fetchFavoritos() {
        val uid = usuarioId ?: return
        viewModelScope.launch {
            repository.fetchFavoritos(uid)
                .onFailure { e ->
                    if (_uiState.value is FavoritosUiState.Loading) {
                        _uiState.value = FavoritosUiState.Error(
                            ApiErrorParser.fromException(e as Exception)
                        )
                    }
                }
        }
    }

    /**
     * Toggle do favorito. Atualiza cache local imediatamente (otimista),
     * sincroniza com servidor em background.
     */
    fun toggleFavorito(mercadoId: String, mercado: MercadoDto? = null) {
        val uid = usuarioId ?: return
        viewModelScope.launch {
            repository.toggleFavorito(uid, mercadoId, mercado)
                .onSuccess { agora ->
                    val msg = if (agora) "Adicionado aos favoritos" else "Removido dos favoritos"
                    _toggleEvento.emit(ToggleEvento.Sucesso(mercadoId, agora, msg))
                }
                .onFailure { e ->
                    _toggleEvento.emit(
                        ToggleEvento.Erro(ApiErrorParser.fromException(e as Exception))
                    )
                }
        }
    }

    fun isFavorito(mercadoId: String): Boolean =
        _favoritoIds.value.contains(mercadoId)

    fun syncPendentes() {
        viewModelScope.launch { repository.syncPendentes() }
    }

    // ── Factory ───────────────────────────────────────────────────────────────
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FavoritosViewModel(context, FavoritoRepository(context)) as T
    }
}

sealed class FavoritosUiState {
    object Loading : FavoritosUiState()
    object Vazio : FavoritosUiState()
    data class Success(val favoritos: List<FavoritoEntity>) : FavoritosUiState()
    data class Error(val msg: String) : FavoritosUiState()
}

sealed class ToggleEvento {
    data class Sucesso(val mercadoId: String, val agora: Boolean, val msg: String) : ToggleEvento()
    data class Erro(val msg: String) : ToggleEvento()
}
