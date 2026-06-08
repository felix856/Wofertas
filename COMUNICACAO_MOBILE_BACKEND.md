# Comunicação Mobile (Android) ↔ Backend — Wofertas

**Data do documento:** 21 de maio de 2026  
**Versão:** 1.0 (análise estática do código-fonte)  
**Público:** desenvolvedores e gestores técnicos (leitura autônoma, sem acesso ao repositório)

---

## 1. Resumo executivo

O aplicativo Android **Wofertas** comunica-se com um backend **Spring Boot** (MongoDB) via **HTTP/JSON**, usando **Retrofit 2** e **OkHttp**, com autenticação **JWT Bearer** persistida em **SharedPreferences**. A base URL é montada em `Constants.kt` (`10.0.2.2:8080` no emulador, IP LAN no dispositivo físico). Os paths declarados em `ApiService.kt` **não usam o prefixo `/api`**, enquanto o frontend web e o `AuthController` usam **`/api/auth`**. Isso gera desalinhamento crítico no login e em rotas de autenticação. Há ainda inconsistências em **favoritos** (`idOferta` no app vs `idMercado` no backend), **geração de JWT** com ordem de parâmetros incorreta em `AuthController`, e regras em `SecurityConfig` que referenciam `/api/ofertas/**` enquanto os controllers expõem `/ofertas/**`. O backend disponibiliza `AndroidCompatibilityController` com rotas legadas (`/usuario/perfil`, `/mercado/cadastro`, etc.) alinhadas ao app. Este documento mapeia cada endpoint, o fluxo de login, segurança e recomendações priorizadas.

---

## 2. Projetos e caminhos

| Papel | Caminho absoluto | Stack principal |
|--------|------------------|-----------------|
| **App Android** | `C:\Users\Felix\Downloads\Wofertas_completo\wofertas_existing\Wofertas` | Kotlin, Retrofit, OkHttp, Gson, SharedPreferences |
| **Backend API** | `C:\Users\Felix\Downloads\DemoCorrigida\Demo_Mongo_teste` | Java 17+, Spring Boot, Spring Security, JWT (jjwt), MongoDB |

**Arquivos-chave analisados**

| Camada | Arquivo |
|--------|---------|
| Android | `app/.../network/ApiService.kt`, `ApiClient.kt`, `utils/Constants.kt`, `AuthManager.kt`, `LoginActivity.kt` |
| Backend | `config/SecurityConfig.java`, `security/JwtUtil.java`, `controller/AuthController.java`, `AndroidCompatibilityController.java`, `FavoritoController.java`, demais `*Controller.java`, `integration/AndroidAppE2EIntegrationTest.java` |
| Web (referência) | `view/js/api/client.js`, `view/js/api/auth.js`, `view/js/api/ofertas.js` |

---

## 3. Diagrama — fluxo de comunicação

```mermaid
sequenceDiagram
    participant U as Usuário
    participant App as Android App
    participant Prefs as SharedPreferences
    participant OK as OkHttp + Retrofit
    participant API as Spring Boot :8080
    participant JWT as JwtAuthFilter
    participant Ctrl as Controllers

    U->>App: email + senha
    App->>OK: POST {BASE_URL}auth/login (publicService)
    OK->>API: HTTP POST
    Note over App,API: App chama /auth/login<br/>Backend expõe /api/auth/login
    API-->>OK: 404 ou 200 (se proxy/alias existir)
    alt Login OK
        OK-->>App: token, id, tipo, email
        App->>Prefs: AuthManager.saveSession()
        App->>U: redireciona (ListaOfertas ou Dashboard)
    end

    U->>App: ação autenticada (ex.: listar ofertas)
    App->>OK: authService(context)
    OK->>Prefs: getToken()
    OK->>API: GET {BASE_URL}ofertas + Authorization Bearer
    API->>JWT: valida Bearer
    JWT->>JWT: extractUsername (subject JWT)
    Note over JWT: Subject deve ser e-mail;<br/>bug se subject = tipo USUARIO/MERCADO
    JWT->>Ctrl: SecurityContext
    Ctrl-->>App: JSON
    alt HTTP 401
        App->>Prefs: clearSession()
        App->>U: LoginActivity
    end
```

---

## 4. Configuração de rede

### 4.1 `BASE_URL` (Android — `Constants.kt`)

| Ambiente | URL padrão | Observação |
|----------|------------|------------|
| **Emulador Android** | `http://10.0.2.2:8080/` | `10.0.2.2` é o alias do `localhost` da máquina host |
| **Dispositivo físico** | `http://192.168.3.177:8080/` | IP fixo no código; **deve ser alterado** para o IP LAN do PC onde o backend roda |
| **Porta** | `8080` | Padrão Spring Boot neste projeto |

Detecção de emulador: heurística em `Build.*` (`goldfish`, `ranchu`, `generic`, etc.).

### 4.2 Alteração dinâmica de IP (`ApiClient.updateBaseUrl`)

- Aceita IP ou URL completa.
- Formato: `http://{ip}:8080/` (barra final obrigatória para Retrofit).
- Invalida cache do `authService` após mudança.

### 4.3 Conectividade no dispositivo real

1. PC e celular na **mesma rede Wi‑Fi**.
2. Firewall do Windows: liberar entrada TCP **8080**.
3. Atualizar IP em `Constants.kt` ou via tela de debug (`ApiDebugActivity`, acessível pelo header em `LoginActivity`).
4. Alternativa com cabo USB: `adb reverse tcp:8080 tcp:8080` e usar `http://127.0.0.1:8080/` no app (comentado no código, não é o default atual).

### 4.4 Backend

- Escuta em `http://0.0.0.0:8080` (acessível na LAN pelo IP do host).
- **Sem** `server.servlet.context-path` configurado: rotas na raiz (`/ofertas`, `/api/auth`, etc.).

---

## 5. Stack de comunicação

| Camada | Tecnologia | Função |
|--------|------------|--------|
| Cliente HTTP | **OkHttp 4** | Timeouts, logging, interceptor JWT, retry |
| API declarativa | **Retrofit 2** | `ApiService` — suspend + `Response<T>` |
| JSON | **Gson** | `yyyy-MM-dd'T'HH:mm:ss` para datas |
| Auth | **JWT** | Header `Authorization: Bearer {token}` |
| Sessão local | **SharedPreferences** (`wofertas_session`) | `token`, `userId`, `email`, `tipo`, cache nome/foto |
| Serviços Retrofit | `ApiClient.publicService` | Login, cadastro, forgot-password (sem token) |
| | `ApiClient.authService(context)` | Demais chamadas; em 401 limpa sessão e abre `LoginActivity` |

**Headers enviados (autenticado):** `Accept: application/json`, `User-Agent: Wofertas-Android-App`, `Authorization: Bearer …`

---

## 6. Fluxo de login (passo a passo)

1. **`LoginActivity.onCreate`** — Se `AuthManager.isLoggedIn()`, redireciona por tipo (`MERCADO` → `DashboardSupermercadoActivity`, senão `ListaOfertas`).
2. Usuário preenche e-mail e senha (mín. 6 caracteres); validação local com `Patterns.EMAIL_ADDRESS`.
3. **`ApiClient.publicService.login(LoginRequest(email, senha))`** — `POST auth/login` relativo à `BASE_URL` → ex.: `http://10.0.2.2:8080/auth/login`.
4. **Backend esperado:** `POST /api/auth/login` (`AuthController`) — **path diferente do app** (ver tabela §8).
5. Em sucesso (`LoginResponse`): `AuthManager.saveSession(token, id, email, tipo)`.
6. Toast de sucesso e **`redirecionarPorTipo()`** com flags `NEW_TASK | CLEAR_TASK`.
7. Chamadas seguintes usam **`ApiClient.authService(context)`** com Bearer do `SharedPreferences`.
8. Se resposta **401**: interceptor limpa sessão e reinicia em `LoginActivity`.
9. **Recuperação de senha:** `forgotPassword(email)` → `POST auth/forgot-password?email=…` (público); depois `ResetPasswordActivity` com `reset-password` / `reset-senha`.

**Payload de login (app):** `{ "email": "...", "senha": "..." }` — alinhado ao `AuthRequest` do backend.

**Resposta esperada (`AuthResponse`):** `token`, `id`, `tipo`, `email` (campos usados pelo app).

---

## 7. Tabela de endpoints — App vs Backend

**Legenda de status**

- **OK** — Método e path compatíveis com o controller indicado (podem existir requisitos de auth ou JWT válido).
- **Desalinhado** — Path, prefixo ou parâmetro não bate com o backend.
- **Risco** — Pode falhar em produção (segurança, JWT, auth obrigatória, contrato JSON).

**Exemplo de URL completa (emulador):** `http://10.0.2.2:8080/{path}`  
**Exemplo de URL completa (device, IP do código):** `http://192.168.3.177:8080/{path}`  
**URL canônica do backend (auth):** `http://{host}:8080/api/auth/...`  
**URL canônica do backend (recursos):** `http://{host}:8080/{recurso}/...` (sem `/api`)

| # | Endpoint App (`ApiService.kt`) | Método | URL completa (exemplo emulador) | Controller Backend | Auth | Status |
|---|-------------------------------|--------|-----------------------------------|-------------------|------|--------|
| 1 | `auth/login` | POST | `http://10.0.2.2:8080/auth/login` | `AuthController` → `/api/auth/login` | Público (`/api/auth/**`) | **Desalinhado** (falta `/api`) |
| 2 | `auth/signup` | POST | `…/auth/signup` | `AuthController` → `/api/auth/signup` | Público | **Desalinhado** |
| 3 | `auth/reset-senha` | POST | `…/auth/reset-senha` | `AuthController` → `/api/auth/reset-senha` | Público | **Desalinhado** |
| 4 | `auth/validar-token` | GET | `…/auth/validar-token` | `AuthController` → `/api/auth/validar-token` | Bearer (rota em `api/auth`) | **Desalinhado** |
| 5 | `auth/forgot-password` | POST | `…/auth/forgot-password?email=` | `AuthController` → `/api/auth/forgot-password` | Público | **Desalinhado** |
| 6 | `auth/reset-password` | POST | `…/auth/reset-password` | `AuthController` → `/api/auth/reset-password` | Público | **Desalinhado** |
| 7 | `usuarios` | POST | `…/usuarios` | `UsuarioController` | JWT | **Risco** (cadastro via `publicService` se usado sem token) |
| 8 | `usuarios/{id}` | GET | `…/usuarios/{id}` | `UsuarioController` | JWT | OK |
| 9 | `usuarios/{id}` | PUT | `…/usuarios/{id}` | `UsuarioController` | JWT | OK |
| 10 | `usuario/perfil` | GET | `…/usuario/perfil` | `AndroidCompatibilityController` | JWT | OK |
| 11 | `usuario/atualizar` | PUT | `…/usuario/atualizar` | `AndroidCompatibilityController` | JWT | OK |
| 12 | `usuario/mudar-senha` | POST | `…/usuario/mudar-senha` | `AndroidCompatibilityController` | JWT | OK |
| 13 | `usuarios/{id}/foto` | POST multipart | `…/usuarios/{id}/foto` | `UsuarioController` | JWT | OK |
| 14 | `usuarios/{id}/senha` | PUT | `…/usuarios/{id}/senha` | `UsuarioController` | JWT | OK |
| 15 | `mercados` | POST | `…/mercados` | `MercadoController` | JWT | OK |
| 16 | `mercados/{id}` | GET | `…/mercados/{id}` | `MercadoController` | JWT (GET pode ser público se path fosse `/api/mercados`) | **Risco** (`SecurityConfig` libera só `/api/mercados/**`; controller é `/mercados`) |
| 17 | `mercados` | GET | `…/mercados` | `MercadoController` | idem | **Risco** |
| 18 | `mercados/proximos` | GET | `…/mercados/proximos?lat&lng&raioKm` | `MercadoController` | idem | **Risco** |
| 19 | `mercados/{id}` | PUT | `…/mercados/{id}` | `MercadoController` | JWT | OK |
| 20 | `mercado/cadastro` | POST | `…/mercado/cadastro` | `AndroidCompatibilityController` | Sem `@AuthenticationPrincipal` | OK (criação) |
| 21 | `mercado/perfil` | GET | `…/mercado/perfil` | `AndroidCompatibilityController` | JWT | OK |
| 22 | `mercado/atualizar` | PUT | `…/mercado/atualizar` | `AndroidCompatibilityController` | JWT | OK |
| 23 | `mercado/todas` | GET | `…/mercado/todas?page&size` | `AndroidCompatibilityController` | Não exige principal no método | OK |
| 24 | `mercados/{id}/logo` | POST multipart | `…/mercados/{id}/logo` | `MercadoController` | JWT | OK |
| 25 | `mercados/{id}/senha` | PUT | `…/mercados/{id}/senha` | `MercadoController` | JWT | OK (coberto por `AndroidAppE2EIntegrationTest`) |
| 26 | `ofertas` | GET | `…/ofertas?page&size&ativo` | `OfertaController` | JWT na prática | **Risco** (Security libera `/api/ofertas/**`, não `/ofertas/**`) |
| 27 | `ofertas/proximas` | GET | `…/ofertas/proximas?lat&lng&raioKm` | `OfertaController` | idem | **Risco** |
| 28 | `ofertas` | POST | `…/ofertas` | `OfertaController` | JWT | OK |
| 29 | `ofertas/{id}` | GET | `…/ofertas/{id}` | `OfertaController` | JWT / leitura | **Risco** (público esperado em Security só em `/api/ofertas`) |
| 30 | `ofertas/{id}` | GET | (duplicado `getOferta`) | `OfertaController` | — | OK (mesmo endpoint) |
| 31 | `ofertas/{id}` | PUT | `…/ofertas/{id}` | `OfertaController` | JWT | OK |
| 32 | `ofertas/{id}` | DELETE | `…/ofertas/{id}` | `OfertaController` | JWT | OK |
| 33 | `ofertas/{id}/imagem` | POST multipart `@Part foto` | `…/ofertas/{id}/imagem` | `OfertaController` (`foto` ou `imagem`) | JWT | OK |
| 34 | `ofertas/mercado/{mercadoId}` | GET | `…/ofertas/mercado/{mercadoId}` | `OfertaController` | JWT | OK |
| 35 | `ofertas/historico` | GET | `…/ofertas/historico` | `OfertaController` (ofertas do mercado logado) | JWT | OK |
| 36 | `ofertas/favoritas` | GET | `…/ofertas/favoritas` | `OfertaController` | JWT | OK |
| 37 | `encartes` | POST multipart | `…/encartes` | `EncarteController` | JWT | OK |
| 38 | `encartes/mercado/{mercadoId}` | GET | `…/encartes/mercado/{mercadoId}` | `EncarteController` | JWT / Security | **Risco** (`/api/encartes/**` vs `/encartes`) |
| 39 | `encartes/{id}` | GET | `…/encartes/{id}` | `EncarteController` | idem | **Risco** |
| 40 | `encartes/{id}` | DELETE | `…/encartes/{id}` | `EncarteController` | JWT | OK |
| 41 | `curtidas/toggle/{idOferta}` | POST | `…/curtidas/toggle/{id}` | `CurtidaController` | JWT | OK |
| 42 | `curtidas/verificar/{idOferta}` | GET | `…/curtidas/verificar/{id}` | `CurtidaController` | JWT | OK |
| 43 | `curtidas/usuario` | GET | `…/curtidas/usuario` | `CurtidaController` | JWT | OK |
| 44 | `visualizacoes/registrar/{idOferta}` | POST | `…/visualizacoes/registrar/{id}?origem=ANDROID` | `VisualizacaoController` | Não exige principal no método | **Risco** (POST exige auth em `anyRequest().authenticated()`) |
| 45 | `interacoes/{tipo}` | POST | `…/interacoes/{tipo}?ofertaId&usuarioId&origem` | `AndroidCompatibilityController` | JWT | OK |
| 46 | `carrinho/adicionar` | POST | `…/carrinho/adicionar` | `CarrinhoController` | JWT | OK |
| 47 | `carrinho/{id}` | DELETE | `…/carrinho/{id}` | `CarrinhoController` | JWT | OK |
| 48 | `carrinho/usuario` | GET | `…/carrinho/usuario` | `CarrinhoController` | JWT | OK |
| 49 | `carrinho/{id}` | PUT | `…/carrinho/{id}` | `CarrinhoController` | JWT | OK |
| 50 | `favoritos/{idOferta}` | POST | `…/favoritos/{idOferta}` | `FavoritoController` → `/{idMercado}` | JWT | **Desalinhado** (semântica oferta vs mercado) |
| 51 | `favoritos/{idOferta}` | DELETE | `…/favoritos/{idOferta}` | `FavoritoController` → `/{idMercado}` | JWT | **Desalinhado** |
| 52 | `favoritos` | GET | `…/favoritos` | `FavoritoController` | JWT | OK |
| 53 | `favoritos/{idUsuario}` | GET | `…/favoritos/{idUsuario}` | `FavoritoController` | JWT | OK |
| 54 | `favoritos/toggle/{idMercado}` | POST | `…/favoritos/toggle/{idMercado}` | `FavoritoController` | JWT | OK (`FavoritosViewModel` usa mercado) |
| 55 | `favoritos/check/{idMercado}` | GET | `…/favoritos/check/{idMercado}` | `FavoritoController` | JWT | OK |
| 56 | `analytics/dashboard` | GET | `…/analytics/dashboard` | `AnalyticsController` | JWT (mercado) | OK |
| 57 | `usuarios/fcm-token` | POST | `…/usuarios/fcm-token` | `AndroidCompatibilityController` (stub) | JWT | OK (no-op no backend) |

### 7.1 Destaques de desalinhamento

#### Prefixo `/api` (autenticação)

| Cliente | Base + path login |
|---------|-------------------|
| Android | `http://10.0.2.2:8080/` + `auth/login` |
| Backend | `http://localhost:8080/api/auth/login` |
| Web (`client.js`) | `http://localhost:8080/api` + `/auth/login` |

**Correção típica no Android:** `BASE_URL = "http://10.0.2.2:8080/api/"` **ou** prefixar só rotas de auth com `api/` no `ApiService`.

#### Favoritos: `idOferta` vs `idMercado`

- Backend (`FavoritoController`): favoritos são de **mercado** — paths `/{idMercado}`, `/toggle/{idMercado}`, `/check/{idMercado}`.
- App: `adicionarFavorito` / `removerFavorito` usam `{idOferta}`; `toggle` / `check` usam `{idMercado}` (correto).
- `FavoritosViewModel` e fluxo principal usam **mercado**; métodos com `idOferta` são legado/inconsistentes.

#### `JwtUtil.generateToken` — ordem de parâmetros

Assinatura em `JwtUtil.java`:

```java
public String generateToken(String userId, String email, String tipo)
```

Chamada em `AuthController.login` (incorreta):

```java
jwtUtil.generateToken(user.getId(), user.getTipo(), user.getUsername());
// Passa: userId, "USUARIO"|"MERCADO", email real
// Subject JWT = tipo; claim "tipo" = email
```

Chamada correta esperada:

```java
jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTipo());
```

`JwtAuthFilter` usa `extractUsername(token)` como **e-mail** para `loadUserByUsername`. Com o bug, o subject pode ser `"USUARIO"` ou `"MERCADO"`, quebrando autenticação nas rotas protegidas.

**Teste E2E** (`AndroidAppE2EIntegrationTest`) repete o erro:

```java
jwtUtil.generateToken(id, tipo, email); // deveria ser (id, email, tipo)
```

---

## 8. Diferença em relação ao frontend web

| Aspecto | Android | Frontend web (`view/js/api/`) |
|---------|---------|-------------------------------|
| Base URL | `http://{host}:8080/` | `http://localhost:8080/api` |
| Login | `POST auth/login` | `POST /auth/login` → **`/api/auth/login`** |
| Ofertas | `GET ofertas` | `GET /ofertas` → **`/api/ofertas`** (path relativo ao `/api`) |
| Controllers reais | `/ofertas`, `/mercados`, `/favoritos` (sem `/api`) | Cliente assume prefixo `/api` em tudo |
| Páginas legadas | — | Alguns scripts usam `http://localhost:8080/mercados` **sem** `/api` (`mercado-cadastro.js`) |
| Reset senha (HTML) | — | `reset-senha.js` chama `http://localhost:8080/auth/...` **sem** `/api` (igual ao app) |

**Conclusão:** Web e Android **divergem entre si** e do mapeamento real dos controllers. Apenas **Auth**, **Chatbot**, **Debug** e **InteracaoController** usam `@RequestMapping` com `/api/...` no backend. Recursos principais estão na **raiz** (`/ofertas`, `/usuarios`, …). O `SecurityConfig` foi escrito assumindo `/api/ofertas/**`, o que **não coincide** com `OfertaController` (`/ofertas`).

---

## 9. `SecurityConfig` — o que é público

Arquivo: `src/main/java/com/example/demo/config/SecurityConfig.java`

| Regra | Paths | Métodos | Efeito |
|-------|-------|---------|--------|
| Preflight | `/**` | OPTIONS | Permitido |
| Autenticação API | `/api/auth/**` | Todos | **Público** (login, signup, reset, etc.) |
| Páginas HTML | `/`, `/login.html`, cadastros | GET | Público |
| Leitura “pública” (config) | `/api/ofertas/**`, `/api/mercados/**`, `/api/encartes/**` | GET | Permitido na config |
| Estáticos | `/css/**`, `/js/**`, `/assets/**`, `/imagens/**`, `/uploads/**` | GET | Público |
| Demais | Qualquer outro | Todos | **`authenticated()`** (JWT Bearer) |

**Implicações para o app Android**

1. Rotas **`/auth/*` sem `/api`** → **não** entram em `permitAll` → login pode retornar **401/403** antes de chegar ao controller (se o path existisse).
2. Rotas reais **`GET /ofertas`**, **`GET /mercados`** → **não** batem com `/api/ofertas/**` → na config atual exigem **JWT**.
3. Rotas **`/usuario/*`**, **`/mercado/*`** (compat Android) → exigem JWT.
4. CORS: origens explícitas (Live Server 5500/5501, localhost:8080) — **app nativo não usa CORS**; impacto só no browser.

---

## 10. Recomendações priorizadas

| Prioridade | Ação | Motivo |
|------------|------|--------|
| **P0** | Corrigir `AuthController`: `generateToken(id, email, tipo)` | JWT inválido para `JwtAuthFilter` |
| **P0** | Unificar prefixo: Android `BASE_URL` com `/api/` **ou** `@RequestMapping("/api")` nos controllers de recurso | Login e web quebrados sem isso |
| **P0** | Alinhar `SecurityConfig` aos paths reais (`/ofertas/**` ou mover controllers para `/api`) | GET público e auth coerentes |
| **P1** | Remover ou corrigir `favoritos/{idOferta}` → `idMercado` no `ApiService` | Evita favoritar ID errado |
| **P1** | Corrigir `AndroidAppE2EIntegrationTest` token: `(id, email, tipo)` | Testes não validam JWT real de login |
| **P2** | Documentar IP LAN no README; manter `ApiClient.updateBaseUrl` | Dispositivo físico |
| **P2** | Adicionar teste E2E `POST /api/auth/login` + chamada autenticada | Regressão mobile |
| **P3** | Consolidar duplicata `getOferta` / `buscarOfertaPorId` | Manutenção |
| **P3** | Implementar ou remover stub `usuarios/fcm-token` | Expectativa de push |

---

## 11. Como testar a conexão rapidamente

### 11.1 Backend no ar

```powershell
cd C:\Users\Felix\Downloads\DemoCorrigida\Demo_Mongo_teste
.\mvnw spring-boot:run
```

### 11.2 Health via browser ou curl (na máquina host)

```powershell
curl -s -o NUL -w "%{http_code}" http://localhost:8080/api/auth/login
```

Esperado: **405** (Method Not Allowed em GET) ou **400** — indica que o servidor responde. **404** em `http://localhost:8080/auth/login` confirma falta do prefixo `/api`.

### 11.3 Login (path correto do backend)

```powershell
curl -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"seu@email.com\",\"senha\":\"suaSenha\"}"
```

Copiar `token` da resposta e testar:

```powershell
curl http://localhost:8080/ofertas ^
  -H "Authorization: Bearer SEU_TOKEN"
```

### 11.4 Do emulador Android

- `BASE_URL` = `http://10.0.2.2:8080/` (atual).
- Após correção P0/P0-prefixo: `http://10.0.2.2:8080/api/`.
- Tela **ApiDebugActivity** (toque no header da login) para trocar IP.

### 11.5 Testes automatizados existentes

```powershell
cd C:\Users\Felix\Downloads\DemoCorrigida\Demo_Mongo_teste
.\mvnw test -Dtest=AndroidAppE2EIntegrationTest
```

Cobre troca de senha `PUT /usuarios/{id}/senha` e `PUT /mercados/{id}/senha` com JWT gerado manualmente (verificar ordem dos parâmetros no token).

---

## 12. Referência rápida — controllers backend

| Controller | `@RequestMapping` | Observação |
|------------|-------------------|------------|
| `AuthController` | `api/auth` | Login, signup, reset |
| `AndroidCompatibilityController` | (raiz) | Perfil usuário/mercado, FCM stub, interações |
| `UsuarioController` | `/usuarios` | CRUD, foto, senha |
| `MercadoController` | `/mercados` | CRUD, logo, proximos, senha |
| `OfertaController` | `/ofertas` | CRUD, proximas, favoritas, historico |
| `EncarteController` | `/encartes` | PDF multipart |
| `FavoritoController` | `/favoritos` | Mercado (não oferta) |
| `CurtidaController` | `/curtidas` | Toggle, verificar, usuario |
| `VisualizacaoController` | `/visualizacoes` | Registrar, count |
| `CarrinhoController` | `/carrinho` | Carrinho do usuário |
| `AnalyticsController` | `/analytics` | Dashboard mercado |
| `InteracaoController` | `/api/interacoes` | Rota web alternativa (`visualizar`) |
| `ChatbotController` | `/api/chatbot` | Não exposto no `ApiService` Android |

---

## 13. Glossário

| Termo | Significado |
|-------|-------------|
| **BASE_URL** | URL base Retrofit (deve terminar com `/`) |
| **Bearer** | Esquema `Authorization: Bearer {JWT}` |
| **publicService** | Retrofit sem interceptor de token |
| **authService** | Retrofit com token e tratamento 401 |
| **ObjectId** | Identificador MongoDB como `String` no app |

---

*Documento gerado por análise estática dos repositórios Android Wofertas e backend Demo_Mongo_teste. Recomenda-se revalidar após alterações em `ApiService.kt`, `SecurityConfig.java` ou `AuthController.java.*
