package com.example.wofertas.viewmodels

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wofertas.data.repository.MercadoRepository
import com.example.wofertas.data.repository.OfertaRepository
import com.example.wofertas.network.MercadoDto
import com.example.wofertas.network.ApiErrorParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para Mapa.kt.
 *
 * Lógica de fallback:
 *   1. Tenta GET /mercados/proximos (quando implementado no backend)
 *   2. Se não disponível → GET /mercados + ordena localmente por Haversine
 *
 * Uso:
 *   viewModel.uiState.collect { state -> ... }
 *   viewModel.ofertasPorMercado.collect { mapa -> ... }
 */
class MapaViewModel(
    private val mercadoRepository: MercadoRepository,
    private val ofertaRepository: OfertaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapaUiState>(MapaUiState.Loading)
    val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

    // Mapa de mercadoId → lista de nomes de ofertas (para o bottom-sheet)
    private val _ofertasPorMercado = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val ofertasPorMercado: StateFlow<Map<String, List<String>>> = _ofertasPorMercado.asStateFlow()

    /**
     * Carrega mercados próximos à localização fornecida.
     * Aceita null (sem localização) — retorna todos os mercados sem ordenar.
     */
    fun carregarMercados(localizacao: Location? = null, raioKm: Double = 10.0) {
        viewModelScope.launch {
            _uiState.value = MapaUiState.Loading

            val result = if (localizacao != null) {
                mercadoRepository.fetchMercadosProximos(
                    lat    = localizacao.latitude,
                    lng    = localizacao.longitude,
                    raioKm = raioKm
                )
            } else {
                mercadoRepository.fetchMercadosOrdenados()
            }

            result
                .onSuccess { lista ->
                    _uiState.value = MapaUiState.Success(lista)
                }
                .onFailure { e ->
                    _uiState.value = MapaUiState.Error(
                        ApiErrorParser.fromException(e as Exception)
                    )
                }
        }
    }

    /**
     * Carrega ofertas e agrupa por mercadoId para exibir no bottom-sheet.
     * Chame após carregarMercados().
     */
    fun carregarOfertasParaMapa() {
        viewModelScope.launch {
            ofertaRepository.fetchOfertas()
                .onSuccess { lista ->
                    val agrupado = lista
                        .groupBy { it.mercadoId ?: "" }
                        .mapValues { entry -> entry.value.mapNotNull { it.nome } }
                    _ofertasPorMercado.value = agrupado
                }
                .onFailure { /* silencioso — mapa funciona sem ofertas */ }
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MapaViewModel(
                MercadoRepository(context),
                OfertaRepository(context)
            ) as T
    }
}

sealed class MapaUiState {
    object Loading : MapaUiState()
    data class Success(val mercados: List<MercadoDto>) : MapaUiState()
    data class Error(val msg: String) : MapaUiState()
}
