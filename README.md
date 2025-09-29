
# wofertas-api

> API backend para o aplicativo Android Wofertas. Gerencia ofertas, tablóides e autenticação de usuários, integrando banco de dados SQLite e Firebase.

## Como rodar

1. Coloque o arquivo JSON do Firebase Service Account na raiz do projeto (ex: `service-account.json`).
2. Copie `.env.example` para `.env` e ajuste variáveis se necessário.
3. Instale as dependências:
	```sh
	npm install
	```
4. Inicie o servidor:
	```sh
	npm run dev
	```

## Estrutura e Classes Principais

### app.js
Ponto de entrada da API. Configura o Express, middlewares, rotas e inicializa a conexão com o banco de dados. Faz o roteamento principal para `/api` (ofertas) e serve arquivos estáticos de uploads.

### server.js
Exemplo alternativo de inicialização, focado nas rotas de tablóides (`/tabloides`).

### config/database.js
Configura e exporta a conexão Sequelize com SQLite (ou outro banco, conforme .env). Usada por todos os modelos que precisam acessar o banco de dados.

### config/firebase.js
Inicializa o Firebase Admin SDK usando as credenciais do service account. Exporta o objeto `admin` (para autenticação de usuários) e `bucket` (para upload de arquivos no Firebase Storage).

### models/Offer.js
Define o modelo `Offer` (Oferta) com Sequelize, representando as ofertas cadastradas no app. Campos: título, descrição, PDF, preço, cidade, localização, validade, etc.

### controllers/offerController.js
Classe que implementa a lógica das rotas de ofertas:
- `index`: lista ofertas com filtros e paginação
- `show`: retorna detalhes de uma oferta
- `create`: cria nova oferta (com upload de PDF)
- `update`: edita oferta existente
- `destroy`: remove oferta
Interage com o modelo `Offer` e responde às requisições do app Android.

### controllers/tabloideController.js
Funções para listar tablóides (mock) e fazer upload de tablóides em PDF para o Firebase Storage. Gera URLs públicas para acesso aos arquivos.

### routes/offerRoutes.js
Define as rotas REST para `/api/offers` e delega para o `offerController`. Usa o middleware de autenticação Firebase e o Multer para upload de arquivos.

### routes/tabloides.js
Define rotas para `/tabloides`:
- GET `/tabloides`: lista tablóides
- POST `/tabloides/upload`: upload de PDF (protegido por autenticação)
Usa Multer (armazenamento em memória) e autenticação.

### middleware/firebaseAuth.js
Middleware que valida o token JWT do Firebase enviado pelo app Android no header Authorization. Decodifica o usuário e injeta em `req.user`.

### middleware/auth.js
Versão alternativa do middleware de autenticação, exportando a função `verifyToken` (usada nas rotas de tablóides).

## Como as classes interagem

- O app Android faz requisições HTTP para a API (ex: criar oferta, listar ofertas, enviar tablóide).
- O token de autenticação do Firebase é enviado no header Authorization.
- O middleware de autenticação (`firebaseAuth.js` ou `auth.js`) valida o token e permite acesso às rotas protegidas.
- As rotas (`offerRoutes.js`, `tabloides.js`) recebem as requisições e delegam para os controllers.
- Os controllers (`offerController.js`, `tabloideController.js`) executam a lógica de negócio, acessando o banco via Sequelize (`Offer.js`) ou o Firebase Storage (`firebase.js`).
- As respostas são enviadas em JSON para o app Android consumir.

## Integração com o app Android Wofertas

- O app Android autentica o usuário via Firebase Auth e obtém um token JWT.
- Para acessar rotas protegidas (criar/editar/remover ofertas, upload de tablóides), o app envia o token no header Authorization: `Bearer <token>`.
- Para uploads, o app envia arquivos PDF via multipart/form-data (campo `pdf` para ofertas, `arquivo` para tablóides).
- O app consome os endpoints REST para listar, buscar, criar, editar e remover ofertas, bem como para listar e enviar tablóides.
- URLs de PDFs e tablóides são retornadas pela API e podem ser exibidas ou baixadas no app.

## Endpoints principais

- `GET  /api/offers` — Lista ofertas
- `GET  /api/offers/:id` — Detalhe de uma oferta
- `POST /api/offers` — Cria oferta (multipart: campo 'pdf' + fields title, description, ownerId...)
- `PUT  /api/offers/:id` — Edita oferta (multipart opcional)
- `DELETE /api/offers/:id` — Remove oferta
- `GET  /tabloides` — Lista tablóides
- `POST /tabloides/upload` — Upload de tablóide (PDF, autenticado)

---
Em caso de dúvidas, consulte os arquivos de cada módulo ou abra uma issue.
