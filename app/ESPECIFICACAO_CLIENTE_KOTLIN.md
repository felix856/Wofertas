# ESPECIFICAÇÃO COMPLETA: Cliente Android Kotlin para API Wofertas

## 📋 SUMÁRIO EXECUTIVO

**Objetivo:** Criar um cliente Android nativo em Kotlin que se integre 100% com a API REST Spring Boot MongoDB (Wofertas).

**Tipo de Projeto:** Aplicativo Android Mobile (min SDK 24)  
**Linguagem:** Kotlin  
**Arquitetura:** MVVM + Repository Pattern + Dependency Injection  
**Autenticação:** JWT (Bearer Token armazenado em SharedPreferences)

---

## 1️⃣ FLUXOS DE NEGÓCIO CRÍTICOS

### 1.1 Fluxo de Autenticação
```
LOGIN USUARIO/MERCADO
├── Input: email + senha
├── POST /auth/login
├── Response: { token, id, nome, tipo(USUARIO|MERCADO), email }
├── Salvar token em SharedPreferences (chave: "auth_token")
├── Salvar tipo em SharedPreferences (chave: "user_type")
├── Validação: Se token nulo/vazio → lançar exceção
├── Timeout: 30 segundos
└── Retry: 3 tentativas com backoff exponencial

LOGOUT
├── Input: nenhum
├── Ação: Limpar SharedPreferences (token + user_type + dados do usuário)
├── Redirecionar para LoginActivity
└── Sem chamada de API (limpeza local apenas)

RESET SENHA
├── Input: email
├── POST /auth/reset-senha
├── Response: mensagem de sucesso (email enviado)
├── O backend envia email com token de reset
├── Usuário clica no link do email e define nova senha
└── Nova senha é persistida no backend
```

### 1.2 Fluxo de Usuário (USUARIO)
```
SIGNUP USUARIO
├── Input: email, senha, nome, tipo="USUARIO"
├── Validação Local:
│   ├── email: format RFC 5322 + min 5 chars
│   ├── senha: min 8 chars + 1 maiúscula + 1 número + 1 especial
│   ├── nome: min 3 chars, max 100 chars
│   └── email NOT already in use (check local cache)
├── POST /auth/signup
├── Response: { id, email, nome, tipo, ativo, dataCriacao }
├── Auto-login: Fazer POST /auth/login com credenciais fornecidas
├── Salvar token retornado
└── Validação Backend: Email único, não aceita duplicatas

LISTAR OFERTAS
├── Endpoint: GET /ofertas?page=0&size=20&ativo=true
├── Paginação: padrão 20 items por página
├── Filtros opcionais: page (default 0), size (default 20), ativo (default true)
├── Cache Local: Armazenar últimos 100 ofertas em Room DB
├── Response: Array de OfertaResponse com campos completos
├── Timeout: 15 segundos
├── Se offline: Retornar dados em cache
└── Pull-to-refresh: Limpar cache e recarregar

CURTIR/DESCURTIR OFERTA
├── Input: idOferta
├── POST /curtidas/toggle/{idOferta}
├── Response: "Curtida adicionada" / "Curtida removida"
├── Validação: idOferta não null/vazio
├── Timeout: 10 segundos
├── UI Update: Incrementar/decrementar ícone de coração em tempo real
└── Otimista: Atualizar UI antes da resposta, reverter se erro

REGISTRAR VISUALIZAÇÃO
├── Input: idOferta, origem (DASHBOARD|FEED|BUSCA)
├── POST /visualizacoes/registrar/{idOferta}?origem=DASHBOARD
├── Validação: idOferta !== null/empty
├── Evento disparado: Toda vez que usuário abre detalhes de oferta
├── Timeout: 5 segundos (assíncrono, não bloqueia)
├── Se falhar: Silenciosa (log mas não notifica usuário)
└── Frequência: 1 visualização por oferta por sessão (máximo)

CARRINHO
├── Adicionar Item:
│   ├── Input: idOferta, quantidade (min 1, max 999)
│   ├── POST /carrinho/adicionar
│   ├── Response: ItemCarrinhoResponse { id, idOferta, quantidade, dataAdicao }
│   ├── Validação: quantidade > 0
│   └── UI: Badge com contador atualizado em tempo real
├── Remover Item:
│   ├── DELETE /carrinho/{idCarrinho}
│   ├── Response: mensagem sucesso/erro
│   └── UI: Remover do adapter
├── Listar Carrinho:
│   ├── GET /carrinho/usuario
│   ├── Response: Array de 0-N itens
│   ├── Cache: Sincronizar ao abrir tela
│   └── Total Calculado: SUM(quantidade * preço) do item
└── Atualizar Quantidade:
    ├── PUT /carrinho/{idCarrinho}
    ├── Body: { idOferta, quantidade }
    └── Response: ItemCarrinhoResponse atualizado

FAVORITOS
├── Adicionar:
│   ├── POST /favoritos/{idOferta}
│   ├── Response: FavoritoResponse { id, idOferta, dataCriacao }
│   └── UI: Ícone star preenchido
├── Remover:
│   ├── DELETE /favoritos/{idOferta}
│   ├── Response: mensagem sucesso
│   └── UI: Ícone star vazio
└── Listar:
    ├── GET /favoritos
    ├── Response: Array de FavoritoResponse
    ├── Cache: Room DB, sincronizar quando favoritos abertos
    └── Sincronização: Comparar timestamps para atualizar
```

### 1.3 Fluxo de Mercado (MERCADO)
```
SIGNUP MERCADO
├── Input: nome, email, senha, telefone, endereco, descricao
├── Validação Local:
│   ├── email: format RFC 5322
│   ├── senha: min 8 chars + 1 maiúscula + 1 número + 1 especial
│   ├── nome: 3-100 chars
│   ├── telefone: 10-11 dígitos (BR)
│   ├── endereco: 5-200 chars
│   └── descricao: 5-500 chars
├── POST /mercado/cadastro
├── Body: MercadoCadastroRequest { nome, email, senha, telefone, endereco, descricao }
├── Response: MercadoResponse { id, nome, email, logo, ativo, dataCriacao }
├── Auto-login: POST /auth/login com email/senha
└── Validação Backend: Email único, verifica campos obrigatórios

CRUD OFERTAS (Mercado)
├── CREATE:
│   ├── POST /ofertas
│   ├── Body: { nome, descricao, preco, desconto(0-100), dataInicio, dataFim }
│   ├── Validação Local:
│   │   ├── nome: 3-200 chars
│   │   ├── descricao: 10-1000 chars
│   │   ├── preco: > 0
│   │   ├── desconto: 0-100%
│   │   └── dataFim > dataInicio
│   ├── Response: OfertaResponse completo
│   └── Upload imagem: POST separado após criação
├── READ:
│   ├── GET /ofertas/{id}
│   ├── Response: OfertaResponse completo com imagem
│   └── Cache: 30 minutos
├── UPDATE:
│   ├── PUT /ofertas/{id}
│   ├── Body: OfertaUpdateRequest (todos campos nullable)
│   ├── Validação: Mesmas regras de CREATE
│   ├── Response: OfertaResponse atualizado
│   └── Otimista: Atualizar UI antes de resposta
├── DELETE:
│   ├── DELETE /ofertas/{id}
│   ├── Response: mensagem sucesso
│   ├── Transição: Marcar como "inativo" no backend (soft delete)
│   └── UI: Remover da lista após confirmação
└── LIST (do próprio mercado):
    ├── GET /ofertas/mercado/{mercadoId}
    ├── Paginação: page, size (20 default)
    ├── Filtros: ativo (true/false)
    ├── Cache: 5 minutos
    └── Response: Array de OfertaResponse

UPLOAD IMAGEM OFERTA
├── Input: File (JPEG/PNG, max 5MB)
├── Validação:
│   ├── Mime-type: image/jpeg | image/png
│   ├── Tamanho: ≤ 5MB
│   └── Dimensões: min 400x400px, max 4000x4000px
├── POST /ofertas/{idOferta}/imagem
├── Body: MultipartBody.Part (file field name: "imagem")
├── Response: OfertaResponse com URL da imagem
├── Storage Backend: MongoDB GridFS ou blob storage (transparente para cliente)
├── Compressão: Comprimir imagem no cliente antes de upload
└── Timeout: 45 segundos

DASHBOARD ANALYTICS (Mercado APENAS)
├── Endpoint: GET /analytics/dashboard
├── Auth: Apenas tipo=MERCADO tem acesso
├── Response: DashboardAnalyticsDTO
│   ├── totalVisualizacoes: Long
│   ├── totalCurtidas: Long
│   ├── totalItensAdicionadosCarrinho: Long
│   ├── taxaConversao: Double (%)
│   ├── produtoPreferencias: Map<String, Long> (nome -> contagem)
│   ├── topOfertasPorVisualizacao: List<OfertaAnalyticsDTO>
│   ├── topOfertasPorCurtidas: List<OfertaAnalyticsDTO>
│   ├── topOfertasPorCarrinho: List<OfertaAnalyticsDTO>
│   └── vizOrigem: Map<String, Long> (DASHBOARD|FEED|BUSCA -> contagem)
├── Cache: 60 segundos (auto-refresh)
├── Timeout: 20 segundos
├── Privacidade: ZERO PII retornado (sem emails, IDs de usuários, etc)
└── Gráficos:
    ├── Gauge: Taxa de conversão (%)
    ├── Bar chart: Top 5 ofertas por métrica
    ├── Pie chart: Distribuição por origem
    ├── Line chart: Trending (últimas 24h, se disponível)
    └── Table: Detalhe de cada oferta com métricas
```

---

## 2️⃣ MODELOS DE DADOS (Data Classes)

### 2.1 Requisições (Request DTOs)
```kotlin
// ========== AUTENTICAÇÃO ==========
data class LoginRequest(
    val email: String,      // email@domain.com
    val senha: String       // min 8 chars
)

data class SignupRequest(
    val email: String,      // email@domain.com
    val senha: String,      // min 8 chars
    val nome: String,       // 3-100 chars
    val tipo: String        // "USUARIO" ou "MERCADO"
)

data class ResetSenhaRequest(
    val email: String,      // email@domain.com
    val token: String,      // token enviado por email
    val novaSenha: String   // min 8 chars
)

// ========== USUARIO ==========
data class AtualizarUsuarioRequest(
    val nome: String?,              // nullable
    val dataNascimento: String?,    // yyyy-MM-dd, nullable
    val telefone: String?,          // nullable
    val endereco: String?           // nullable
)

data class MudarSenhaRequest(
    val senhaAtual: String,         // senha atual
    val novaSenha: String,          // min 8 chars
    val confirmarSenha: String      // igual novaSenha
)

// ========== MERCADO ==========
data class MercadoCadastroRequest(
    val nome: String,       // 3-100 chars
    val email: String,      // email@domain.com
    val senha: String,      // min 8 chars
    val telefone: String,   // 10-11 dígitos
    val endereco: String,   // 5-200 chars
    val descricao: String   // 5-500 chars
)

data class MercadoUpdateRequest(
    val nome: String?,              // nullable
    val telefone: String?,          // nullable
    val endereco: String?,          // nullable
    val descricao: String?,         // nullable
    val logo: String?               // URL ou base64, nullable
)

// ========== OFERTAS ==========
data class OfertaCreateRequest(
    val nome: String,                       // 3-200 chars
    val descricao: String,                  // 10-1000 chars
    val preco: Double,                      // > 0
    val desconto: Int,                      // 0-100
    val dataInicio: LocalDateTime,          // ISO 8601
    val dataFim: LocalDateTime              // > dataInicio
)

data class OfertaUpdateRequest(
    val nome: String?,                      // nullable
    val descricao: String?,                 // nullable
    val preco: Double?,                     // nullable
    val desconto: Int?,                     // nullable
    val dataInicio: LocalDateTime?,         // nullable
    val dataFim: LocalDateTime?,            // nullable
    val ativo: Boolean?                     // nullable
)

// ========== CARRINHO ==========
data class ItemCarrinhoRequest(
    val idOferta: String,           // ID válido da oferta
    val quantidade: Int             // 1-999
)

// ========== FAVORITOS ==========
// Sem request, apenas POST /favoritos/{idOferta}
```

### 2.2 Respostas (Response DTOs)
```kotlin
// ========== AUTENTICAÇÃO ==========
data class LoginResponse(
    val token: String,      // JWT Bearer Token
    val id: String,         // ID do usuário/mercado
    val nome: String,       // Nome do usuário/mercado
    val tipo: String,       // "USUARIO" ou "MERCADO"
    val email: String       // Email autenticado
)

// ========== USUARIO ==========
data class UsuarioResponse(
    val id: String,
    val email: String,
    val nome: String,
    val tipo: String,              // "USUARIO" ou "MERCADO"
    val dataNascimento: LocalDate?,
    val telefone: String?,
    val endereco: String?,
    val ativo: Boolean,
    val dataCriacao: LocalDateTime,
    val dataAtualizacao: LocalDateTime?
)

// ========== MERCADO ==========
data class MercadoResponse(
    val id: String,
    val nome: String,
    val email: String,
    val telefone: String?,
    val endereco: String?,
    val logo: String?,              // URL da imagem
    val descricao: String?,
    val ativo: Boolean,
    val dataCriacao: LocalDateTime,
    val dataAtualizacao: LocalDateTime?
)

// ========== OFERTAS ==========
data class OfertaResponse(
    val id: String,
    val mercadoId: String,          // ID do mercado que criou
    val nome: String,
    val descricao: String,
    val preco: Double,
    val desconto: Int,              // 0-100
    val dataInicio: LocalDateTime,
    val dataFim: LocalDateTime,
    val imagem: String?,            // URL ou null
    val ativo: Boolean,
    val dataCriacao: LocalDateTime,
    val dataAtualizacao: LocalDateTime?
)

data class OfertaAnalyticsDTO(
    val nome: String,
    val visualizacoes: Long,
    val curtidas: Long,
    val itensCarrinho: Long,
    val desconto: Int,
    val origem: String              // "DASHBOARD" | "FEED" | "BUSCA"
)

// ========== ANALYTICS ==========
data class DashboardAnalyticsDTO(
    val totalVisualizacoes: Long,
    val totalCurtidas: Long,
    val totalItensAdicionadosCarrinho: Long,
    val taxaConversao: Double,      // 0.0-100.0
    val produtoPreferencias: Map<String, Long>,     // nome -> qtd
    val topOfertasPorVisualizacao: List<OfertaAnalyticsDTO>,
    val topOfertasPorCurtidas: List<OfertaAnalyticsDTO>,
    val topOfertasPorCarrinho: List<OfertaAnalyticsDTO>,
    val vizOrigem: Map<String, Long>                // "DASHBOARD" -> qtd
)

// ========== CURTIDAS ==========
data class CurtidaResponse(
    val id: String,
    val idOferta: String,
    val dataCurtida: LocalDateTime
)

// ========== VISUALIZAÇÕES ==========
data class VisualizacaoResponse(
    val id: String,
    val idOferta: String,
    val dataVisualizacao: LocalDateTime,
    val origem: String             // "DASHBOARD" | "FEED" | "BUSCA"
)

// ========== CARRINHO ==========
data class ItemCarrinhoResponse(
    val id: String,
    val idUsuario: String,
    val idOferta: String,
    val nomeOferta: String,
    val mercadoId: String,
    val quantidade: Int,
    val dataAdicao: LocalDateTime
)

// ========== FAVORITOS ==========
data class FavoritoResponse(
    val id: String,
    val idOferta: String,
    val dataCriacao: LocalDateTime
)

// ========== ERRO ==========
data class ErrorResponse(
    val mensagem: String,
    val codigo: String,             // "AUTH_INVALID" | "NOT_FOUND" | etc
    val timestamp: LocalDateTime
)
```

---

## 3️⃣ INTERFACE RETROFIT (Endpoints)

```kotlin
interface WofertasAPI {
    
    companion object {
        const val BASE_URL = "http://SEU_IP:8080/"  // Configurável em BuildConfig
    }
    
    // =============== AUTENTICAÇÃO ===============
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): UsuarioResponse
    
    @POST("auth/reset-senha")
    suspend fun resetSenha(@Body request: ResetSenhaRequest): String
    
    @GET("auth/validar-token")
    suspend fun validarToken(): String
    
    // =============== USUARIO ===============
    
    @GET("usuario/perfil")
    suspend fun getPerfil(): UsuarioResponse
    
    @PUT("usuario/atualizar")
    suspend fun atualizarPerfil(@Body request: AtualizarUsuarioRequest): UsuarioResponse
    
    @POST("usuario/mudar-senha")
    suspend fun mudarSenha(@Body request: MudarSenhaRequest): String
    
    // =============== MERCADO ===============
    
    @POST("mercado/cadastro")
    suspend fun criarMercado(@Body request: MercadoCadastroRequest): MercadoResponse
    
    @GET("mercado/perfil")
    suspend fun getMercadoPerfil(): MercadoResponse
    
    @PUT("mercado/atualizar")
    suspend fun atualizarMercado(@Body request: MercadoUpdateRequest): MercadoResponse
    
    @GET("mercado/todas")
    suspend fun listarMercados(): List<MercadoResponse>
    
    @GET("mercado/todas?page={page}&size={size}")
    suspend fun listarMercadosPaginado(
        @Path("page") page: Int,
        @Path("size") size: Int
    ): List<MercadoResponse>
    
    // =============== OFERTAS ===============
    
    @GET("ofertas")
    suspend fun listarOfertas(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("ativo") ativo: Boolean? = null
    ): List<OfertaResponse>
    
    @POST("ofertas")
    suspend fun criarOferta(@Body request: OfertaCreateRequest): OfertaResponse
    
    @GET("ofertas/{id}")
    suspend fun getOferta(@Path("id") id: String): OfertaResponse
    
    @PUT("ofertas/{id}")
    suspend fun atualizarOferta(
        @Path("id") id: String,
        @Body request: OfertaUpdateRequest
    ): OfertaResponse
    
    @DELETE("ofertas/{id}")
    suspend fun deletarOferta(@Path("id") id: String): String
    
    @Multipart
    @POST("ofertas/{id}/imagem")
    suspend fun uploadImagemOferta(
        @Path("id") id: String,
        @Part imagem: MultipartBody.Part
    ): OfertaResponse
    
    @GET("ofertas/mercado/{mercadoId}")
    suspend fun listarOfertasPorMercado(
        @Path("mercadoId") mercadoId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("ativo") ativo: Boolean? = null
    ): List<OfertaResponse>
    
    // =============== CURTIDAS ===============
    
    @POST("curtidas/toggle/{idOferta}")
    suspend fun toggleCurtida(@Path("idOferta") idOferta: String): String
    
    @GET("curtidas/verificar/{idOferta}")
    suspend fun verificarCurtida(@Path("idOferta") idOferta: String): Boolean
    
    @GET("curtidas/usuario")
    suspend fun minhasCurtidas(): List<CurtidaResponse>
    
    // =============== VISUALIZAÇÕES ===============
    
    @POST("visualizacoes/registrar/{idOferta}")
    suspend fun registrarVisualizacao(
        @Path("idOferta") idOferta: String,
        @Query("origem") origem: String = "DASHBOARD"
    ): VisualizacaoResponse
    
    // =============== CARRINHO ===============
    
    @POST("carrinho/adicionar")
    suspend fun adicionarCarrinho(@Body request: ItemCarrinhoRequest): ItemCarrinhoResponse
    
    @DELETE("carrinho/{id}")
    suspend fun removerCarrinho(@Path("id") id: String): String
    
    @GET("carrinho/usuario")
    suspend fun meuCarrinho(): List<ItemCarrinhoResponse>
    
    @PUT("carrinho/{id}")
    suspend fun atualizarCarrinho(
        @Path("id") id: String,
        @Body request: ItemCarrinhoRequest
    ): ItemCarrinhoResponse
    
    // =============== FAVORITOS ===============
    
    @POST("favoritos/{idOferta}")
    suspend fun adicionarFavorito(@Path("idOferta") idOferta: String): FavoritoResponse
    
    @DELETE("favoritos/{idOferta}")
    suspend fun removerFavorito(@Path("idOferta") idOferta: String): String
    
    @GET("favoritos")
    suspend fun meusFavoritos(): List<FavoritoResponse>
    
    // =============== ANALYTICS (MERCADO ONLY) ===============
    
    @GET("analytics/dashboard")
    suspend fun getDashboardAnalytics(): DashboardAnalyticsDTO
}
```

---

## 4️⃣ INFRAESTRUTURA (Repository + DI)

### 4.1 Autenticação & Interceptadores
```kotlin
// TokenManager.kt - Gerencia JWT
class TokenManager(context: Context) {
    private val sharedPref = context.getSharedPreferences("wofertas_auth", Context.MODE_PRIVATE)
    
    fun saveToken(token: String) = sharedPref.edit().putString("token", token).apply()
    fun getToken(): String = sharedPref.getString("token", "") ?: ""
    fun clearToken() = sharedPref.edit().remove("token").apply()
    
    fun isTokenValid(): Boolean = getToken().isNotEmpty()
}

// AuthInterceptor.kt - Adiciona Authorization header
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}

// HttpLoggingInterceptor (para debug)
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
}
```

### 4.2 Retrofit Configuration
```kotlin
class RetrofitClient(context: Context) {
    
    private val tokenManager = TokenManager(context)
    
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .registerTypeAdapter(LocalDateTime::class.java, 
            JsonDeserializer { json, _, _ ->
                LocalDateTime.parse(json.asString)
            }
        )
        .registerTypeAdapter(LocalDate::class.java,
            JsonDeserializer { json, _, _ ->
                LocalDate.parse(json.asString)
            }
        )
        .create()
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenManager))
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val api: WofertasAPI = Retrofit.Builder()
        .baseUrl(WofertasAPI.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build()
        .create(WofertasAPI::class.java)
}
```

### 4.3 Repository Pattern
```kotlin
class WofertasRepository(
    private val api: WofertasAPI,
    private val tokenManager: TokenManager
) {
    
    // ===== AUTENTICAÇÃO =====
    suspend fun login(email: String, senha: String): Result<LoginResponse> = safeCall {
        api.login(LoginRequest(email, senha))
    }
    
    suspend fun signup(email: String, senha: String, nome: String, tipo: String): Result<UsuarioResponse> = safeCall {
        api.signup(SignupRequest(email, senha, nome, tipo))
    }
    
    suspend fun logout() {
        tokenManager.clearToken()
    }
    
    // ===== OFERTAS =====
    suspend fun listarOfertas(page: Int = 0, size: Int = 20): Result<List<OfertaResponse>> = safeCall {
        api.listarOfertas(page, size)
    }
    
    suspend fun criarOferta(request: OfertaCreateRequest): Result<OfertaResponse> = safeCall {
        api.criarOferta(request)
    }
    
    suspend fun uploadImagemOferta(idOferta: String, file: File): Result<OfertaResponse> = safeCall {
        val requestBody = file.asRequestBody("image/*".toMediaType())
        val multipart = MultipartBody.Part.createFormData("imagem", file.name, requestBody)
        api.uploadImagemOferta(idOferta, multipart)
    }
    
    // ===== CARRINHO =====
    suspend fun adicionarCarrinho(idOferta: String, quantidade: Int): Result<ItemCarrinhoResponse> = safeCall {
        api.adicionarCarrinho(ItemCarrinhoRequest(idOferta, quantidade))
    }
    
    suspend fun meuCarrinho(): Result<List<ItemCarrinhoResponse>> = safeCall {
        api.meuCarrinho()
    }
    
    // ===== FAVORITOS =====
    suspend fun adicionarFavorito(idOferta: String): Result<FavoritoResponse> = safeCall {
        api.adicionarFavorito(idOferta)
    }
    
    suspend fun meusFavoritos(): Result<List<FavoritoResponse>> = safeCall {
        api.meusFavoritos()
    }
    
    // ===== ANALYTICS =====
    suspend fun getDashboardAnalytics(): Result<DashboardAnalyticsDTO> = safeCall {
        api.getDashboardAnalytics()
    }
    
    private suspend fun <T> safeCall(call: suspend () -> T): Result<T> = try {
        Result.Success(call())
    } catch (e: Exception) {
        Result.Error(e)
    }
}

sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val exception: Exception) : Result<T>()
}
```

### 4.4 ViewModels com Coroutines
```kotlin
class OfertasViewModel(private val repo: WofertasRepository) : ViewModel() {
    
    private val _ofertas = MutableLiveData<List<OfertaResponse>>()
    val ofertas: LiveData<List<OfertaResponse>> = _ofertas
    
    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun listarOfertas(page: Int = 0) {
        viewModelScope.launch {
            _loading.postValue(true)
            when (val result = repo.listarOfertas(page)) {
                is Result.Success -> {
                    _ofertas.postValue(result.data)
                    _error.postValue(null)
                }
                is Result.Error -> {
                    _error.postValue(result.exception.message ?: "Erro desconhecido")
                }
            }
            _loading.postValue(false)
        }
    }
    
    fun criarOferta(request: OfertaCreateRequest) {
        viewModelScope.launch {
            _loading.postValue(true)
            when (val result = repo.criarOferta(request)) {
                is Result.Success -> {
                    _error.postValue(null)
                    // Refresh lista
                    listarOfertas()
                }
                is Result.Error -> {
                    _error.postValue(result.exception.message)
                }
            }
            _loading.postValue(false)
        }
    }
}

class AuthViewModel(private val repo: WofertasRepository) : ViewModel() {
    
    private val _loginSuccess = MutableLiveData<LoginResponse>()
    val loginSuccess: LiveData<LoginResponse> = _loginSuccess
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun login(email: String, senha: String) {
        viewModelScope.launch {
            when (val result = repo.login(email, senha)) {
                is Result.Success -> {
                    _loginSuccess.postValue(result.data)
                    _error.postValue(null)
                }
                is Result.Error -> {
                    _error.postValue(result.exception.message)
                }
            }
        }
    }
}
```

---

## 5️⃣ VALIDAÇÕES (Regras de Negócio)

### 5.1 Email
- Padrão: RFC 5322 compliant
- Tamanho: 5-254 caracteres
- Exemplos válidos: user@example.com, nome+tag@domain.co.uk
- Exemplos inválidos: user@, @domain.com, user@domain, user domain@example.com

### 5.2 Senha
- Mínimo: 8 caracteres
- Deve conter: 1 maiúscula + 1 minúscula + 1 número + 1 caractere especial (!@#$%^&*)
- Máximo: 128 caracteres
- Não pode conter: espaços em branco, caracteres de controle

### 5.3 Nome
- Tamanho: 3-100 caracteres
- Apenas letras, números, hífens e espaços
- Não pode: começar/terminar com espaço, ter espaços duplos

### 5.4 Oferta
- Nome: 3-200 caracteres
- Descricao: 10-1000 caracteres
- Preço: > 0, até 2 casas decimais
- Desconto: 0-100%
- Data início/fim: ISO 8601 format, fim > início
- Limite de ativas por mercado: sem limite especificado (validar com admin)

### 5.5 Imagem
- Formatos: JPEG, PNG
- Tamanho máximo: 5 MB
- Dimensões: mín 400x400px, máx 4000x4000px
- Compressão: reduzir antes de upload se > 1MB

### 5.6 Carrinho
- Quantidade por item: 1-999
- Itens máximos no carrinho: 50 (recomendação, validar com backend)

---

## 6️⃣ TRATAMENTO DE ERROS

### 6.1 HTTP Status Codes Esperados
```
200 OK              - Sucesso
201 Created         - Recurso criado
204 No Content      - Sucesso sem body
400 Bad Request     - Validação falhou (enviar detalhes ao usuário)
401 Unauthorized    - Token expirado/inválido (redirect login)
403 Forbidden       - Não tem permissão (ex: USUARIO tentando acessar analytics)
404 Not Found       - Recurso não existe (oferta deletada, etc)
409 Conflict        - Email duplicado, ofertas em conflito
429 Too Many Requests - Rate limit (backoff exponencial)
500 Server Error    - Erro genérico (retry com backoff)
503 Service Unavailable - Servidor em manutenção (notificar usuário)
```

### 6.2 Tratamento de Erros
```kotlin
// Padrão Try-Catch
try {
    val result = repo.login(email, senha)
    when (result) {
        is Result.Success -> navigateToHome()
        is Result.Error -> {
            when (result.exception) {
                is HttpException -> {
                    val code = result.exception.code()
                    when (code) {
                        401 -> showDialog("Credenciais inválidas")
                        409 -> showDialog("Email já cadastrado")
                        else -> showDialog("Erro ${code}: ${result.exception.message()}")
                    }
                }
                is IOException -> showDialog("Sem conexão à internet")
                else -> showDialog("Erro: ${result.exception.message}")
            }
        }
    }
} catch (e: Exception) {
    Log.e("AuthViewModel", "Erro não tratado", e)
}

// Retry com Backoff Exponencial
private suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    call: suspend () -> T
): T {
    var lastException: Exception? = null
    repeat(maxRetries) { attempt ->
        try {
            return call()
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxRetries - 1) {
                delay(initialDelayMs * (2 to the power of attempt))
            }
        }
    }
    throw lastException ?: Exception("Max retries exceeded")
}
```

---

## 7️⃣ CACHE & SINCRONIZAÇÃO

### 7.1 Room Database (Local Cache)
```kotlin
@Entity
data class OfertaCache(
    @PrimaryKey val id: String,
    val nome: String,
    val preco: Double,
    val timestamp: Long  // para invalidar cache
)

@Dao
interface OfertaCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ofertas: List<OfertaCache>)
    
    @Query("SELECT * FROM OfertaCache")
    suspend fun getAll(): List<OfertaCache>
    
    @Query("DELETE FROM OfertaCache WHERE timestamp < :expireTime")
    suspend fun deleteExpired(expireTime: Long)
}
```

### 7.2 Estratégia de Cache
- Ofertas: Cache por 5 minutos
- Favoritos: Cache por 10 minutos
- Carrinho: Sempre sincronizar quando abrir tela
- Analytics: Cache por 60 segundos (auto-refresh)
- Perfil: Cache por 30 minutos

### 7.3 Online First, Offline Fallback
```kotlin
suspend fun listarOfertas(): List<OfertaResponse> {
    return try {
        val remote = api.listarOfertas()
        dao.insertAll(remote.map { it.toCache() })
        remote
    } catch (e: IOException) {
        // Sem internet, retornar cache
        dao.getAll().map { it.toResponse() }
    }
}
```

---

## 8️⃣ SEGURANÇA

### 8.1 Armazenamento de Credenciais
- ❌ NUNCA armazenar senha em SharedPreferences
- ✅ Armazenar APENAS JWT token
- ✅ Usar Android Keystore para dados sensíveis (EncryptedSharedPreferences)

### 8.2 Comunicação
- ✅ SEMPRE usar HTTPS (não HTTP)
- ✅ SSL Pinning (opcional, se aplicação crítica)
- ✅ Adicionar User-Agent customizado

### 8.3 Logout
- Limpar SharedPreferences completamente
- Limpar cache de dados sensíveis
- Redirecionar para LoginActivity
- Não manter referências de objetos sensíveis em memória

---

## 9️⃣ TESTES UNITÁRIOS (Exemplos)

```kotlin
@RunWith(JUnit4::class)
class AuthViewModelTest {
    
    private lateinit var viewModel: AuthViewModel
    private val mockRepo = mockk<WofertasRepository>()
    
    @Before
    fun setup() {
        viewModel = AuthViewModel(mockRepo)
    }
    
    @Test
    fun loginSuccess() = runTest {
        val response = LoginResponse("token123", "user1", "João", "USUARIO", "joao@email.com")
        coEvery { mockRepo.login("joao@email.com", "Senha@123") } returns Result.Success(response)
        
        viewModel.login("joao@email.com", "Senha@123")
        
        assertTrue(viewModel.loginSuccess.getOrAwaitValue() == response)
        assertNull(viewModel.error.getOrAwaitValue())
    }
    
    @Test
    fun loginError() = runTest {
        val exception = Exception("Credenciais inválidas")
        coEvery { mockRepo.login(any(), any()) } returns Result.Error(exception)
        
        viewModel.login("joao@email.com", "WrongPassword")
        
        assertEquals("Credenciais inválidas", viewModel.error.getOrAwaitValue())
    }
}
```

---

## 1️⃣0️⃣ ESTRUTURA DO PROJETO

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/wofertas/
│   │   │   ├── api/
│   │   │   │   ├── WofertasAPI.kt
│   │   │   │   └── RetrofitClient.kt
│   │   │   ├── data/
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/
│   │   │   │   │   │   ├── LoginRequest.kt
│   │   │   │   │   │   ├── OfertaCreateRequest.kt
│   │   │   │   │   │   └── ...
│   │   │   │   │   └── response/
│   │   │   │   │       ├── LoginResponse.kt
│   │   │   │   │       ├── OfertaResponse.kt
│   │   │   │   │       └── ...
│   │   │   │   ├── local/
│   │   │   │   │   ├── WofertasDatabase.kt
│   │   │   │   │   ├── OfertaCacheDao.kt
│   │   │   │   │   └── ...
│   │   │   │   └── repository/
│   │   │   │       └── WofertasRepository.kt
│   │   │   ├── ui/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── LoginActivity.kt
│   │   │   │   │   ├── LoginViewModel.kt
│   │   │   │   │   ├── SignupActivity.kt
│   │   │   │   │   └── SignupViewModel.kt
│   │   │   │   ├── ofertas/
│   │   │   │   │   ├── OfertasFragment.kt
│   │   │   │   │   ├── OfertasViewModel.kt
│   │   │   │   │   ├── OfertaDetalheActivity.kt
│   │   │   │   │   └── OfertaAdapter.kt
│   │   │   │   ├── carrinho/
│   │   │   │   │   ├── CarrinhoFragment.kt
│   │   │   │   │   ├── CarrinhoViewModel.kt
│   │   │   │   │   └── CarrinhoItemAdapter.kt
│   │   │   │   ├── analytics/
│   │   │   │   │   ├── DashboardActivity.kt
│   │   │   │   │   └── DashboardViewModel.kt
│   │   │   │   └── common/
│   │   │   │       ├── BaseActivity.kt
│   │   │   │       ├── LoadingDialog.kt
│   │   │   │       └── ErrorDialog.kt
│   │   │   ├── security/
│   │   │   │   ├── TokenManager.kt
│   │   │   │   ├── AuthInterceptor.kt
│   │   │   │   └── EncryptedPreferences.kt
│   │   │   └── util/
│   │   │       ├── Constants.kt
│   │   │       ├── DateFormatter.kt
│   │   │       └── ImageCompressor.kt
│   │   └── res/
│   │       ├── layout/
│   │       ├── drawable/
│   │       ├── values/
│   │       └── anim/
│   └── test/java/com/example/wofertas/
│       ├── viewmodel/
│       │   ├── AuthViewModelTest.kt
│       │   └── OfertasViewModelTest.kt
│       └── repository/
│           └── WofertasRepositoryTest.kt
└── build.gradle.kts
```

---

## 1️⃣1️⃣ FLUXOS DE TELA

### Login
1. User abre app
2. Se token válido em SharedPreferences → vai para HomeActivity
3. Se sem token → abre LoginActivity
4. User insere email + senha
5. Validação local
6. POST /auth/login
7. Se sucesso → salvar token + ir para HomeActivity
8. Se erro → mostrar mensagem de erro

### Home (USUARIO)
1. RecyclerView com ofertas (infinite scroll)
2. Cada card mostra: imagem, nome, preço original, preço com desconto, coração (curtida), star (favorito)
3. Clique na card → OfertaDetalheActivity
4. Pull-to-refresh limpa cache e recarrega
5. Menu inferior: Ofertas, Carrinho, Favoritos, Perfil

### Detalhe da Oferta
1. Imagem maior no topo
2. Nome, descrição, preço
3. Botão "Adicionar ao Carrinho" + Botão "Curtir" + Botão "Favoritar"
4. Stats: visualizações, curtidas (sem revelar identidade)
5. POST /visualizacoes/registrar ao abrir tela

### Carrinho
1. Lista de itens com: imagem, nome, preço, quantidade (+-), subtotal
2. Cálculo automático: Total = SUM(quantidade * preço)
3. Botão "Limpar Carrinho"
4. Botão "Finalizar Compra" (conecta com gateway, não implementado neste spec)

### Dashboard Analytics (MERCADO)
1. Gauge: Taxa de conversão (%)
2. Cards: Total visualizações, Total curtidas, Total adições carrinho
3. Bar chart: Top 5 ofertas por métrica
4. Pie chart: Distribuição por origem (DASHBOARD|FEED|BUSCA)
5. Tabela: Detalhe de cada oferta
6. Auto-refresh a cada 60 segundos

---

## 1️⃣2️⃣ DEPENDÊNCIAS build.gradle.kts

```kotlin
dependencies {
    // Kotlin & Coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Android
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    
    // HTTP Client
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Local Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.auth0:java-jwt:4.4.0")
    
    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    
    // Charts (Analytics)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test:runner:1.5.2")
}
```

---

## 1️⃣3️⃣ CONFIGURAÇÃO build.gradle.kts

```kotlin
android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.wofertas"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        buildConfigField("String", "API_BASE_URL", "\"http://SEU_IP:8080/\"")
    }
    
    buildTypes {
        debug {
            debuggable = true
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.X:8080/\"")
        }
        release {
            debuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", "\"https://api.wofertas.com/\"")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

---

## 1️⃣4️⃣ AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.CAMERA" />
    
    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="false">
        
        <activity
            android:name=".ui.auth.LoginActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <activity android:name=".ui.auth.SignupActivity" android:exported="false" />
        <activity android:name=".ui.home.HomeActivity" android:exported="false" />
        <activity android:name=".ui.ofertas.OfertaDetalheActivity" android:exported="false" />
        
    </application>
</manifest>
```

---

**Essa é a especificação completa para outro programador/IA criar o cliente Kotlin 100% funcional com a API Wofertas. Inclui regras de negócio, fluxos, DTOs, endpoints, validações, tratamento de erros, cache, segurança e testes.**
