package com.example.wofertas.network

import com.google.gson.annotations.SerializedName

// ══════════════════════════════════════════════════════════════════════════════
// PADRÃO DE RESPOSTA REPOSITORY
// ══════════════════════════════════════════════════════════════════════════════

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Exception) : ApiResult<Nothing>()

    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (Exception) -> Unit): ApiResult<T> {
        if (this is Error) action(exception)
        return this
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// REQUESTS
// ══════════════════════════════════════════════════════════════════════════════

data class LoginRequest(val email: String, val senha: String)

data class SignupRequest(
    val email: String,
    val senha: String,
    val nome: String,
    val tipo: String // "USUARIO" ou "MERCADO"
)

data class CadastroUsuarioRequest(val nome: String, val email: String, val senha: String)

data class CadastroMercadoRequest(
    val nome: String,
    val email: String,
    val senha: String,
    val telefone: String? = null,
    val endereco: String,
    val descricao: String = "",
    val cnpj: String? = null,
    val imagemLogo: String? = ""
)

data class ResetPasswordRequest(val email: String, val token: String, val novaSenha: String)
data class MudarSenhaRequest(val senhaAtual: String, val novaSenha: String, val confirmarSenha: String)

// Modelo para troca de senha nos endpoints /usuarios/{id}/senha e /mercados/{id}/senha.
data class TrocarSenhaRequest(
    val senhaAtual: String,
    val novaSenha: String,
    val confirmacao: String = novaSenha
)

data class OfertaRequest(
    val nome: String,
    val status: String,
    val data: String, // yyyy-MM-dd
    val imagemOferta: String?
)

data class ItemCarrinhoRequest(val idOferta: String, val quantidade: Int)

data class ChatbotRequest(
    val mensagem: String,
    val pagina: String? = null,
    val contextoTela: String? = null
)

// ANALYTICS REQUEST
data class InteracaoRequest(
    val ofertaId: String,
    val usuarioId: String,
    val origem: String = "ANDROID"
)

// ══════════════════════════════════════════════════════════════════════════════
// RESPONSES
// ══════════════════════════════════════════════════════════════════════════════

data class LoginResponse(
    val token: String,
    val id: String,
    val tipo: String,
    val email: String,
    val nome: String? = null
)

data class UsuarioResponse(
    val id: String,
    val email: String,
    val nome: String,
    val tipo: String = "USUARIO",
    val imagemPerfil: String? = null,
    val ativo: Boolean = true
)

data class MercadoResponse(
    val id: String,
    val nome: String,
    val email: String,
    val telefone: String? = null,
    val endereco: String? = null,
    val imagemLogo: String? = null,
    val logo: String? = null,
    val descricao: String? = null,
    val ativo: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cnpj: String? = null,
    val dataCriacao: String? = null,
    val dataAtualizacao: String? = null
)

data class MercadoResumoDto(
    val id: String,
    val nome: String,
    val cnpj: String? = null,
    val endereco: String? = null,
    val imagemLogo: String? = null,
    val email: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class OfertaResponse(
    val id: String,
    @SerializedName("mercadoId") val mercadoId: String? = null,
    val nome: String,
    val status: String? = "ATIVO",
    val data: String? = null,
    val dataFim: String? = null,
    val imagemOferta: String? = null,
    val mercado: MercadoResumoDto? = null,
    val visualizacoes: Long = 0,
    val curtidas: Long = 0,
    val imagem: String? = null,
    val descricao: String? = null,
    val preco: Double? = null,
    val desconto: Int? = null,
    val dataInicio: String? = null,
    val ativo: Boolean? = null,
    val dataCriacao: String? = null,
    val dataAtualizacao: String? = null
)

data class FavoritoResponse(
    val id: String,
    @SerializedName("idUsuario") val idUsuario: String,
    @SerializedName("idMercado") val idMercado: String
)

data class CurtidaResponse(val id: String, val idOferta: String, val dataCurtida: String)
data class VisualizacaoResponse(val id: String, val idOferta: String, val dataVisualizacao: String, val origem: String)
data class ItemCarrinhoResponse(
    val id: String, val idUsuario: String, val idOferta: String,
    val nomeOferta: String, val mercadoId: String, val quantidade: Int, val dataAdicao: String
)

data class OfertaAnalyticsDto(
    val id: String = "",
    val nome: String = "",
    val status: String? = null,
    val imagemOferta: String? = null,
    val curtidas: Long = 0,
    val visualizacoes: Long = 0,
    val itensCarrinho: Long = 0,
    val engajamento: Double = 0.0
)

data class InsightEstrategicoDto(
    val encarteMelhorPerformance: String? = null,
    val engajamentoMedio: Double = 0.0,
    val recomendacao: String? = null,
    val clientesAtivos: Long = 0,
    val tendencia: String? = null
)

data class DashboardAnalyticsDto(
    val totalVisualizacoes: Long = 0,
    val totalCurtidas: Long = 0,
    val totalFavoritos: Long = 0,
    @SerializedName(value = "totalItensCarrinho", alternate = ["totalItensAdicionadosCarrinho"])
    val totalItensCarrinho: Long = 0,
    val totalEncartes: Long = 0,
    val taxaConversaoVisualizacoesCurtidas: Double = 0.0,
    val taxaConversaoVisualizacoesCarrinho: Double = 0.0,
    val encartesRanking: List<OfertaAnalyticsDto> = emptyList(),
    val encartesComMaiorCurtidas: List<OfertaAnalyticsDto> = emptyList(),
    val encartesComMaiorCarrinho: List<OfertaAnalyticsDto> = emptyList(),
    val produtosComMaiorCurtidas: Map<String, Long> = emptyMap(),
    val produtosComMaiorCarrinho: Map<String, Long> = emptyMap(),
    val visualizacoesPorOrigem: Map<String, Long> = emptyMap(),
    val insight: InsightEstrategicoDto? = null
)

data class ChatbotResponse(
    val resposta: String,
    val tipoUsuario: String? = null,
    val modo: String? = null,
    val modelo: String? = null,
    val contextoAnaliticoUsado: Boolean = false,
    val respondidoEm: String? = null
)

data class MercadoRankingDto(
    val id: String,
    val nome: String,
    val imagemLogo: String? = null,
    val totalCurtidas: Long = 0,
    val totalFavoritos: Long = 0,
    val posicao: Int = 0
)

data class EncarteDto(
    val id: String,
    val mercadoId: String,
    val titulo: String,
    val urlPdf: String,
    val dataCriacao: String
)

data class BackendErrorDto(
    val message: String? = null,
    val mensagem: String? = null,
    val codigo: String? = null,
    val status: Int? = null
) {
    fun toUserMessage(): String = mensagem ?: message ?: "Erro desconhecido"
}

data class FcmTokenRequest(
    val usuarioId: String,
    val fcmToken: String
)

data class PrivacyDeletionRequest(
    val email: String? = null,
    val requesterType: String? = null,
    val reason: String? = null,
    val source: String = "ANDROID"
)

data class DataPrivacyRequestDto(
    val id: String? = null,
    val requesterId: String? = null,
    val requesterType: String? = null,
    val email: String? = null,
    val requestType: String? = null,
    val status: String? = null,
    val source: String? = null,
    val requestedAt: String? = null
)

data class ChatbotRequest(
    val mensagem: String,
    val pagina: String? = null,
    val contextoTela: String? = null
)

data class ChatbotResponse(
    val resposta: String? = null,
    val tipoUsuario: String? = null,
    val modo: String? = null,
    val modelo: String? = null,
    val contextoAnaliticoUsado: Boolean = false,
    val respondidoEm: String? = null
)

// ALIASES PARA COMPATIBILIDADE ABSOLUTA
typealias UsuarioDto = UsuarioResponse
typealias MercadoDto = MercadoResponse
typealias OfertaDto = OfertaResponse
typealias FavoritoDto = FavoritoResponse
typealias ResetSenhaRequest = ResetPasswordRequest
typealias MercadoCadastroRequest = CadastroMercadoRequest
typealias OfertaCreateRequest = OfertaRequest
