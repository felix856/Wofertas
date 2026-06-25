# Wofertas

Wofertas e uma plataforma com aplicativo Android, backend Spring Boot/MongoDB e painel web para conectar clientes a ofertas, encartes e supermercados proximos.

## Componentes

- `app/`: aplicativo Android em Kotlin.
- `src/main/java/`: backend Spring Boot com API REST, JWT e MongoDB.
- `view/`: frontend web estatico usado no Vercel e tambem copiado para deploy Docker.
- `PLAY_STORE_LGPD_CHECKLIST.md`: checklist de privacidade, LGPD e Play Store.

## Tecnologias

- Android Kotlin, Retrofit, Room, WorkManager, OSMDroid, ML Kit OCR.
- Java 21, Spring Boot, Spring Security, JWT e Spring Data MongoDB.
- Frontend HTML/CSS/JavaScript.

## Rodar backend localmente

1. Instale JDK 21.
2. Rode MongoDB local em `mongodb://localhost:27017/wofertas` ou ajuste `src/main/resources/application-dev.properties`.
3. Execute:

```bash
./mvnw spring-boot:run
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Rodar Android

1. Abra o projeto no Android Studio.
2. Confirme o SDK local em `local.properties`:

```properties
sdk.dir=C\:\\Users\\Felix\\AppData\\Local\\Android\\Sdk
```

3. Para testes locais, o build `debug` permite HTTP/local network.
4. Para producao, o build `release` usa HTTPS e bloqueia cleartext.

## Deploy

Backend:

- Configure `SPRING_PROFILES_ACTIVE=prod`.
- Configure `MONGODB_URI`, `JWT_SECRET` e, se necessario, `CORS_ALLOWED_ORIGINS`.

Frontend web:

- Publique a pasta `view/` no Vercel.
- As URLs de privacidade esperadas estao em `PLAY_STORE_LGPD_CHECKLIST.md`.

## Validacao local

```powershell
.\mvnw.cmd test
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

## Observacoes Play Store

- Gere um AAB assinado para publicar; o APK unsigned e apenas artefato local.
- Antes da primeira publicacao, considere trocar `applicationId = "com.example.wofertas"` por um identificador definitivo.
- Mantenha o formulario Data Safety coerente com as praticas reais do app.
