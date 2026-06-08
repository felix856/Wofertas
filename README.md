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
