package com.example.wofertas.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface que define todos os endpoints da API Wofertas
 */
interface ApiService {

    // =============== AUTENTICAÇÃO ===============

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<UsuarioResponse>

    @POST("auth/reset-senha")
    suspend fun resetSenha(@Body request: ResetPasswordRequest): Response<String>

    @GET("auth/validar-token")
    suspend fun validarToken(): Response<String>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Query("email") email: String): Response<Void>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<Void>

    // =============== USUÁRIO ===============

    @POST("usuarios")
    suspend fun cadastrarUsuario(@Body body: CadastroUsuarioRequest): Response<UsuarioDto>

    @GET("usuarios/{id}")
    suspend fun getUsuario(@Path("id") id: String): Response<UsuarioDto>

    @PUT("usuarios/{id}")
    suspend fun atualizarUsuario(
        @Path("id") id: String,
        @Body body: CadastroUsuarioRequest
    ): Response<UsuarioDto>

    @GET("usuario/perfil")
    suspend fun getPerfil(): Response<UsuarioDto>

    @PUT("usuario/atualizar")
    suspend fun atualizarPerfil(@Body request: Map<String, @JvmSuppressWildcards Any?>): Response<UsuarioDto>

    @POST("usuario/mudar-senha")
    suspend fun mudarSenha(@Body request: MudarSenhaRequest): Response<String>

    @Multipart
    @POST("usuarios/{id}/foto")
    suspend fun uploadFotoUsuario(
        @Path("id") id: String,
        @Part foto: MultipartBody.Part
    ): Response<UsuarioDto>

    @PUT("usuarios/{id}/senha")
    suspend fun trocarSenhaUsuario(
        @Path("id") id: String,
        @Body body: TrocarSenhaRequest
    ): Response<Void>

    // =============== MERCADO ===============

    @POST("mercados")
    suspend fun cadastrarMercado(@Body body: CadastroMercadoRequest): Response<MercadoDto>

    @GET("mercados/{id}")
    suspend fun getMercado(@Path("id") id: String): Response<MercadoDto>

    @GET("mercados")
    suspend fun listarMercados(): Response<List<MercadoDto>>

    @GET("mercados/proximos")
    suspend fun listarMercadosProximos(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("raioKm") raioKm: Double = 10.0
    ): Response<List<MercadoDto>>

    @PUT("mercados/{id}")
    suspend fun atualizarMercado(
        @Path("id") id: String,
        @Body body: CadastroMercadoRequest
    ): Response<MercadoDto>

    @POST("mercado/cadastro")
    suspend fun criarMercado(@Body request: MercadoCadastroRequest): Response<MercadoDto>

    @GET("mercado/perfil")
    suspend fun getMercadoPerfil(): Response<MercadoDto>

    @PUT("mercado/atualizar")
    suspend fun atualizarMercadoRequest(@Body request: Map<String, @JvmSuppressWildcards Any?>): Response<MercadoDto>

    @GET("mercado/todas")
    suspend fun listarMercadosPaginado(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<List<MercadoDto>>

    @Multipart
    @POST("mercados/{id}/logo")
    suspend fun uploadLogoMercado(
        @Path("id") id: String,
        @Part logo: MultipartBody.Part
    ): Response<MercadoDto>

    @PUT("mercados/{id}/senha")
    suspend fun trocarSenhaMercado(
        @Path("id") id: String,
        @Body body: TrocarSenhaRequest
    ): Response<Void>

    // =============== OFERTAS ===============

    @GET("ofertas")
    suspend fun listarOfertas(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("ativo") ativo: Boolean? = null
    ): Response<List<OfertaDto>>

    @GET("ofertas/proximas")
    suspend fun listarOfertasProximas(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("raioKm") raioKm: Double = 10.0,
        @Query("ativo") ativo: Boolean? = true
    ): Response<List<OfertaDto>>

    @POST("ofertas")
    suspend fun criarOferta(@Body request: OfertaRequest): Response<OfertaDto>

    @GET("ofertas/{id}")
    suspend fun buscarOfertaPorId(@Path("id") id: String): Response<OfertaDto>

    @GET("ofertas/{id}")
    suspend fun getOferta(@Path("id") id: String): Response<OfertaDto>

    @PUT("ofertas/{id}")
    suspend fun atualizarOferta(
        @Path("id") id: String,
        @Body request: OfertaRequest
    ): Response<OfertaDto>

    @DELETE("ofertas/{id}")
    suspend fun deletarOferta(@Path("id") id: String): Response<Void>

    @Multipart
    @POST("ofertas/{id}/imagem")
    suspend fun uploadImagemOferta(
        @Path("id") id: String,
        @Part foto: MultipartBody.Part
    ): Response<OfertaDto>

    @GET("ofertas/mercado/{mercadoId}")
    suspend fun listarOfertasPorMercado(
        @Path("mercadoId") mercadoId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("ativo") ativo: Boolean? = null
    ): Response<List<OfertaDto>>

    @GET("ofertas/historico")
    suspend fun listarMinhasOfertas(): Response<List<OfertaDto>>

    @GET("ofertas/favoritas")
    suspend fun listarOfertasFavoritas(): Response<List<OfertaDto>>

    // =============== ENCARTES ===============

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

    // =============== CURTIDAS ===============

    @POST("curtidas/toggle/{idOferta}")
    suspend fun toggleCurtida(@Path("idOferta") idOferta: String): Response<String>

    @GET("curtidas/verificar/{idOferta}")
    suspend fun verificarCurtida(@Path("idOferta") idOferta: String): Response<Boolean>

    @GET("curtidas/usuario")
    suspend fun minhasCurtidas(): Response<List<CurtidaResponse>>

    // =============== VISUALIZAÇÕES ===============

    @POST("visualizacoes/registrar/{idOferta}")
    suspend fun registrarVisualizacao(
        @Path("idOferta") idOferta: String,
        @Query("origem") origem: String = "ANDROID"
    ): Response<VisualizacaoResponse>

    // =============== ANALYTICS ===============

    @POST("interacoes/{tipo}")
    suspend fun registrarInteracao(
        @Path("tipo") tipo: String,
        @Query("ofertaId") ofertaId: String,
        @Query("usuarioId") usuarioId: String,
        @Query("origem") origem: String = "ANDROID"
    ): Response<Void>

    // =============== CARRINHO ===============

    @POST("carrinho/adicionar")
    suspend fun adicionarCarrinho(@Body request: ItemCarrinhoRequest): Response<ItemCarrinhoResponse>

    @DELETE("carrinho/{id}")
    suspend fun removerCarrinho(@Path("id") id: String): Response<Void>

    @GET("carrinho/usuario")
    suspend fun meuCarrinho(): Response<List<ItemCarrinhoResponse>>

    @PUT("carrinho/{id}")
    suspend fun atualizarCarrinho(
        @Path("id") id: String,
        @Body request: ItemCarrinhoRequest
    ): Response<ItemCarrinhoResponse>

    // =============== FAVORITOS ===============

    @POST("favoritos/{idOferta}")
    suspend fun adicionarFavorito(@Path("idOferta") idOferta: String): Response<FavoritoDto>

    @DELETE("favoritos/{idOferta}")
    suspend fun removerFavorito(@Path("idOferta") idOferta: String): Response<Void>

    @GET("favoritos")
    suspend fun meusFavoritos(): Response<List<FavoritoDto>>

    @GET("favoritos/{idUsuario}")
    suspend fun listarFavoritos(@Path("idUsuario") idUsuario: String): Response<List<FavoritoDto>>

    @POST("favoritos/toggle/{idMercado}")
    suspend fun toggleFavorito(@Path("idMercado") idMercado: String): Response<FavoritoDto>

    @GET("favoritos/check/{idMercado}")
    suspend fun checkFavorito(@Path("idMercado") idMercado: String): Response<Boolean>

    // =============== ANALYTICS (MERCADO ONLY) ===============

    @GET("analytics/dashboard")
    suspend fun getDashboardAnalytics(): Response<DashboardAnalyticsDto>

    @GET("analytics/ranking-mercados")
    suspend fun getRankingMercados(): Response<List<MercadoRankingDto>>

    @POST("usuarios/fcm-token")
    suspend fun registrarFcmToken(@Body body: FcmTokenRequest): Response<Void>
}
