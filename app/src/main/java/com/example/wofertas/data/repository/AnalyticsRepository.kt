package com.example.wofertas.data.repository

import android.content.Context
import android.util.Log
import com.example.wofertas.AuthManager
import com.example.wofertas.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Repositório responsável pelo rastreamento de interações do usuário (Analytics).
 * Implementa o padrão 'fire and forget' para não impactar a experiência do usuário.
 */
class AnalyticsRepository(private val context: Context) {

    private val api get() = ApiClient.authService(context)
    
    // Escopo global para garantir que o evento seja disparado mesmo se o chamador for destruído
    private val analyticsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Registra um evento de interação no servidor.
     * @param ofertaId ID da oferta ou recurso interagido.
     * @param tipoInteracao Tipo da interação (VISUALIZACAO, CLIQUE_CARRINHO, CURTIDA).
     */
    fun trackEvent(ofertaId: String, tipoInteracao: String) {
        val usuarioId = AuthManager.getUserId(context) ?: return
        
        analyticsScope.launch {
            try {
                val response = api.registrarInteracao(
                    tipo = tipoInteracao,
                    ofertaId = ofertaId,
                    usuarioId = usuarioId,
                    origem = "ANDROID"
                )
                if (!response.isSuccessful) {
                    Log.w("Analytics", "Falha ao registrar interacao: ${response.code()}")
                }
            } catch (e: Exception) {
                // 'Fire and forget': falhas no analytics não devem afetar o fluxo do app
                Log.e("Analytics", "Erro de rede ao rastrear evento: ${e.message}")
            }
        }
    }

    companion object {
        const val VISUALIZACAO = "VISUALIZACAO"
        const val CLIQUE_CARRINHO = "CLIQUE_CARRINHO"
        const val CURTIDA = "CURTIDA"
    }
}
