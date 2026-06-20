<<<<<<< HEAD
# Wofertas - API (Backend) e Dashboard Web

Este repositório contém a infraestrutura do backend do aplicativo **Wofertas**, construído em **Spring Boot (Java)** com banco de dados **MongoDB**.
Além disso, esta aplicação já serve o Frontend (Dashboard Web HTML/JS/CSS) nativamente.

## Tecnologias
* **Java 21**
* **Spring Boot 3**
* **MongoDB** (Spring Data MongoDB)
* **Spring Security + JWT** (Autenticação)

## Como rodar localmente
1. Tenha o JDK 21 e o Maven instalados.
2. Certifique-se de que o MongoDB está rodando ou adicione sua URL local.
3. Configure as variáveis de ambiente necessárias ou apenas modifique o arquivo `application-dev.properties` para testes locais.
4. Execute:
   ```bash
   ./mvnw spring-boot:run
   ```
5. O Web Dashboard estará acessível em `http://localhost:8080/`.

## Hospedagem e Deploy (Render)

Esta API possui um `Dockerfile` preparado para deploy rápido na plataforma **Render.com**.

**Atenção:** Como este repositório possui a API em uma branch específica, **configure a opção `Branch` no painel do Render para `backend`**.

### Variáveis de Ambiente Necessárias (Production):
No painel do Render (Environment Variables), adicione:
- `SPRING_PROFILES_ACTIVE`: `prod`
- `MONGODB_URI`: Sua string de conexão do MongoDB Atlas (ex: `mongodb+srv://...`)
- `MONGODB_DATABASE`: Opcional (Padrão: `wofertas`)
- `JWT_SECRET`: Senha forte gerada aleatoriamente para criptografia de tokens.

> [!WARNING]
> O plano gratuito do Render não possui persistência de disco (arquivos salvos localmente).
> Como a pasta de `uploads` salva imagens em disco, a cada deploy ou restart do servidor, as imagens enviadas anteriormente serão apagadas temporariamente (com exceção das imagens hospedadas remotamente). Para resolver isso futuramente, considere hospedar as imagens em S3 ou Cloudinary.
=======
# Wofertas - Aplicativo Android

Este é o aplicativo Android oficial para a plataforma Wofertas, projetado para permitir que clientes e supermercados interajam, visualizem ofertas, gerenciem listas de compras e publiquem encartes.

## Tecnologias Usadas
* **Kotlin** (Linguagem Principal)
* **Retrofit** (Cliente HTTP para comunicação com a API)
* **Coroutines** (Para processamento assíncrono)
* **Glide / Coil** (Para carregamento de imagens)
* **OSMDroid** (Mapas)

## Configuração do Ambiente

1. Abra o projeto no **Android Studio**.
2. O aplicativo consome a API REST que precisa estar rodando (localmente ou no Render).
3. Vá no arquivo `app/src/main/java/com/example/wofertas/network/ApiEnvironment.kt` e defina o ambiente correto na função `default()`.
   * Para Produção (Render): `PRODUCTION`
   * Para Testes Locais no PC (Emulador): `EMULATOR`
   * Para Testes no Celular Físico: `PHYSICAL_DEVICE` (Atualize o IP local).

## Integração com o Backend (Render)

Quando o backend for implantado no [Render](https://render.com/), atualize a URL de produção na classe `ApiEnvironment.kt`:

```kotlin
PRODUCTION("https://SEU-APP-RENDER.onrender.com/", "Production Server")
```

Após alterar, sincronize o Gradle e clique em **Run** ou **Build APK**.
>>>>>>> f493123585e4c68ca236493080c7eb79a0086d7f
