package com.example.wofertas.network

import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * ADIÇÕES ao ApiService.kt existente.
 *
 * COMO USAR: Cole os métodos abaixo dentro da interface ApiService existente.
 * Este arquivo existe apenas como referência de quais endpoints faltam.
 */
interface ApiServiceAdditions {

    // ─── ENCARTES (faltam no ApiService atual) ───────────────────────────────

    @Multipart
    @POST("encartes")
    suspend fun uploadEncarte(
        @Part("mercadoId") mercadoId: RequestBody,
        @Part("titulo")    titulo:    RequestBody,
        @Part            pdf:       MultipartBody.Part
    ): Response<EncarteDto>

    @GET("encartes/mercado/{mercadoId}")
    suspend fun getEncartesByMercado(
        @Path("mercadoId") mercadoId: String
    ): Response<List<EncarteDto>>

    @GET("encartes/{id}")
    suspend fun getEncarteById(
        @Path("id") id: String
    ): Response<EncarteDto>

    @DELETE("encartes/{id}")
    suspend fun deleteEncarte(
        @Path("id") id: String
    ): Response<Void>

    // ─── FCM TOKEN (opcional — adicionar endpoint no backend) ────────────────
    @POST("usuarios/fcm-token")
    suspend fun registrarFcmToken(@Body body: FcmTokenRequest): Response<Void>
}

// ─── UTILITÁRIO: Parse de erro do backend ────────────────────────────────────
object ApiErrorParser {

    private val gson = Gson()

    /**
     * Converte o corpo de erro JSON do Spring Boot em mensagem amigável.
     *
     * Uso:
     *   if (!response.isSuccessful) {
     *       val msg = ApiErrorParser.parse(response)
     *       Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
     *   }
     */
    fun <T> parse(response: Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val err = gson.fromJson(errorBody, BackendErrorDto::class.java)
                err.toUserMessage()
            } else {
                httpCodeToMessage(response.code())
            }
        } catch (e: Exception) {
            httpCodeToMessage(response.code())
        }
    }

    fun fromException(e: Exception): String = when {
        e.message?.contains("timeout", ignoreCase = true) == true ->
            "Tempo limite esgotado. Verifique sua conexão."
        e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
            "Sem conexão com o servidor. Verifique sua internet ou o endereço da API."
        e.message?.contains("Connection refused", ignoreCase = true) == true ->
            "Servidor recusou a conexão. Verifique se o backend está rodando."
        else -> "Erro de rede: ${e.message ?: "desconhecido"}"
    }

    private fun httpCodeToMessage(code: Int) = when (code) {
        400  -> "Dados inválidos enviados ao servidor."
        401  -> "Sessão expirada. Faça login novamente."
        403  -> "Você não tem permissão para esta ação."
        404  -> "Recurso não encontrado no servidor."
        409  -> "Conflito: registro já existe."
        422  -> "Dados não processáveis. Verifique os campos."
        500  -> "Erro interno do servidor. Tente novamente em instantes."
        503  -> "Servidor temporariamente indisponível."
        else -> "Erro $code. Tente novamente."
    }
}
