# Wofertas Android

Aplicativo Android do Wofertas, desenvolvido em Kotlin no Android Studio.

## Backend

O app se comunica com o backend Spring Boot `Demo_Mongo_teste` na porta `8080`.

- Emulador Android Studio: `http://10.0.2.2:8080/`
- Celular fisico na mesma rede Wi-Fi: `http://192.168.3.177:8080/`

Se o IPv4 do computador mudar, atualize:

- `app/src/main/java/com/example/wofertas/utils/Constants.kt`
- `app/src/main/java/com/example/wofertas/network/ApiEnvironment.kt`

## Build

```powershell
.\gradlew.bat :app:assembleDebug
```

APK gerado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub

Nao subir arquivos locais, gerados ou sensiveis:

- `local.properties`
- `service-account.json`
- `*-firebase-adminsdk-*.json`
- APKs/AABs
- `build/`, `outputs/`, `tmp/`
- bancos locais `.sqlite`/`.db`

Esses itens ja estao cobertos pelo `.gitignore`.
