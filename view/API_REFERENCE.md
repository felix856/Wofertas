# 📚 Guia de APIs JavaScript Frontend

**Data**: 2026-05-19  
**Status**: ✅ Fase 4 e 5 Completas

---

## 🎯 Visão Geral

Frontend estruturado em **3 camadas**:

```
┌─────────────────────────────────────┐
│  Telas Interativas (HTML + JS)      │ ← login.html, dashboard.html, etc.
├─────────────────────────────────────┤
│  Serviços Utilitários               │ ← validators, storage, formatters, etc.
├─────────────────────────────────────┤
│  APIs (Endpoints /api/*)            │ ← authAPI, usuariosAPI, ofertasAPI, etc.
├─────────────────────────────────────┤
│  Client HTTP (Base)                 │ ← apiClient com JWT automático
└─────────────────────────────────────┘
```

---

## 📦 Carregamento de Módulos

Todos os HTMLs carregam automaticamente:

```html
<script src="/js/modules.js"></script>
```

Ordem automática:
1. ✅ `client.js` (Client HTTP)
2. ✅ `validators.js`, `storage.js`, `formatters.js`, `errorHandler.js`, `notifications.js`
3. ✅ `auth.js`, `usuarios.js`, `mercados.js`, `ofertas.js`, `favoritos.js`

---

## 🔐 Client HTTP - `apiClient`

Base para todas as requisições com **JWT automático**.

### Métodos

```javascript
// GET
const result = await apiClient.get('/usuarios/perfil');

// POST
const result = await apiClient.post('/ofertas', { nome: 'Promoção', valor: 10 });

// PUT
const result = await apiClient.put(`/ofertas/${id}`, { valor: 15 });

// DELETE
const result = await apiClient.delete(`/ofertas/${id}`);

// Token
apiClient.getToken();        // Recupera JWT
apiClient.setToken(token);   // Armazena JWT
apiClient.clearToken();      // Remove JWT
```

### Retorno

```javascript
{
  data: { /* resposta da API */ },
  status: 200,
  error: null
}
```

---

## 🔑 Auth API - `authAPI`

Autenticação e gerenciamento de sessão.

```javascript
// Login
const result = await authAPI.login(email, senha);
// → { data: { token, id, tipo, email, ... }, status: 200, error: null }

// Logout
authAPI.logout();

// Register
const result = await authAPI.register({
  nome: 'Mercado XYZ',
  email: 'admin@xyz.com',
  senha: 'Senha@123',
  confirmacao: 'Senha@123'
});

// Reset senha
const result = await authAPI.requestPasswordReset('admin@xyz.com');

// Validar token
const result = await authAPI.validateToken();
```

---

## 👥 Usuários API - `usuariosAPI`

Gerenciamento de usuários/mercados.

```javascript
// Perfil do usuário autenticado
const result = await usuariosAPI.getPerfil();

// Obter por ID
const result = await usuariosAPI.getById(id);

// Atualizar dados
const result = await usuariosAPI.update(id, {
  nome: 'Novo Nome',
  email: 'novo@email.com'
});

// Trocar senha
const result = await usuariosAPI.trocaSenha(id, senhaAtual, novaSenha, confirmacao);

// Upload de foto
const file = document.getElementById('foto').files[0];
const result = await usuariosAPI.uploadFoto(id, file);

// Listar todos
const result = await usuariosAPI.listAll();

// Histórico
const result = await usuariosAPI.getHistorico();
```

---

## 🏬 Mercados API - `mercadosAPI`

Gerenciamento de mercados/supermercados.

```javascript
// Listar todos
const result = await mercadosAPI.listAll({
  ativo: true,
  skip: 0,
  limit: 10,
  search: 'Mercado'
});

// Obter por ID
const result = await mercadosAPI.getById(id);

// Criar
const result = await mercadosAPI.create({
  nome: 'Mercado ABC',
  email: 'admin@abc.com',
  endereco: 'Rua X, 123',
  latitude: -23.5505,
  longitude: -46.6333
});

// Atualizar
const result = await mercadosAPI.update(id, {
  nome: 'Novo Nome'
});

// Trocar senha
const result = await mercadosAPI.trocaSenha(id, senhaAtual, novaSenha, confirmacao);

// Proximidade (geoloc)
const result = await mercadosAPI.getProximos(raioKm, latitude, longitude);
// raioKm = 10 (padrão), latitude e longitude do usuário

// Dashboard/Analytics
const result = await mercadosAPI.getDashboard(id);
// Retorna: { totalOfertas, totalFavoritos, totalVisualizacoes, ... }

// Ativar/desativar
const result = await mercadosAPI.setActive(id, true);
```

---

## 🎁 Ofertas API - `ofertasAPI`

Gerenciamento de ofertas e promoções.

```javascript
// Listar
const result = await ofertasAPI.listAll({
  ativo: true,
  mercadoId: 'id-do-mercado',
  skip: 0,
  limit: 20,
  search: 'Promoção'
});

// Obter por ID
const result = await ofertasAPI.getById(id);

// Criar
const result = await ofertasAPI.create({
  nome: 'Promoção Especial',
  descricao: 'Desconto de 20%',
  precoOriginal: 100,
  precoPromocional: 80,
  mercadoId: 'id-do-mercado',
  ativo: true
});

// Atualizar
const result = await ofertasAPI.update(id, {
  precoPromocional: 70
});

// Proximidade
const result = await ofertasAPI.getProximas(raioKm, latitude, longitude);

// Por mercado
const result = await ofertasAPI.getByMercado(mercadoId);

// Por categoria
const result = await ofertasAPI.getByCategoria('Alimentos');

// Ativar/desativar
const result = await ofertasAPI.setActive(id, false);

// Registrar visualização
const result = await ofertasAPI.recordView(id);

// Estatísticas
const result = await ofertasAPI.getStats(id);
```

---

## ⭐ Favoritos API - `favoritosAPI`

Gerenciamento de favoritos.

```javascript
// Meus favoritos
const result = await favoritosAPI.listMeus();

// Adicionar
const result = await favoritosAPI.adicionar(ofertaId);

// Remover
const result = await favoritosAPI.remover(ofertaId);

// É favorito?
const result = await favoritosAPI.isFavorito(ofertaId);

// Contar
const result = await favoritosAPI.contar();

// Exportar
const result = await favoritosAPI.exportar();

// Importar
const result = await favoritosAPI.importar(dadosFavoritos);
```

---

## 🛠️ Serviços Utilitários

### Validadores - `validators`

```javascript
validators.email('test@email.com')          // true/false
validators.senha('Senha@123')               // true/false (8+ chars, 1 maiúscula, 1 número, 1 especial)
validators.telefone('(11)99999-9999')       // true/false
validators.cpf('111.222.333-44')            // true/false
validators.cnpj('00.000.000/0000-00')       // true/false
validators.url('https://example.com')       // true/false
validators.required('texto')                // true/false
validators.minLength('texto', 5)            // true/false
validators.maxLength('texto', 10)           // true/false
validators.range(50, 0, 100)                // true/false
validators.data('01/01/2026')               // true/false (DD/MM/YYYY)
validators.fileType(file, ['image/jpeg'])   // true/false
validators.fileSize(file, 5)                // true/false (em MB)
```

### Storage - `storageService`

```javascript
storageService.set('chave', valor);           // Salva em localStorage
storageService.get('chave');                  // Recupera
storageService.remove('chave');               // Remove
storageService.clear();                       // Limpa tudo
storageService.keys();                        // Lista todas as chaves
storageService.has('chave');                  // Chave existe?

// Com expiração (em segundos)
storageService.setWithExpiry('temp', valor, 3600);  // 1 hora
const valor = storageService.getWithExpiry('temp');  // null se expirado
```

### Formatadores - `formatters`

```javascript
formatters.moeda(100)                    // R$ 100,00
formatters.percentual(0.25)              // 25.00%
formatters.data(new Date())              // 19/05/2026
formatters.numero(1234.56)               // 1.234,56
formatters.telefone('11999999999')       // (11) 99999-9999
formatters.cpf('11122233344')            // 111.222.333-44
formatters.cnpj('00000000000191')        // 00.000.000/0001-91
formatters.cep('01310100')               // 01310-100
formatters.truncar('Texto longo...', 10) // Texto lon...
formatters.tamanhoArquivo(1024)          // 1.00 KB
formatters.tempo(3661)                   // 1h 1m 1s
formatters.capitalize('texto')           // Texto
formatters.slug('Meu Texto')             // meu-texto
formatters.tempoRelativo(new Date())     // há 2 minutos
```

### Error Handler - `errorHandler`

```javascript
errorHandler.debug('mensagem', dados)    // Log DEBUG
errorHandler.info('mensagem')            // Log INFO
errorHandler.warn('mensagem')            // Log WARNING
errorHandler.error('mensagem', dados)    // Log ERROR
errorHandler.fatal('mensagem')           // Log FATAL

errorHandler.handleApiResult(result, 'contexto') // true/false

errorHandler.getErrorMessage(401)        // Mensagem amigável
errorHandler.getHistory()                // Histórico de logs
errorHandler.clearHistory()              // Limpa histórico
errorHandler.exportLogs()                // JSON de logs
```

### Notificações - `notificationsService`

```javascript
notificationsService.success('Operação realizada!', 3000);
notificationsService.error('Erro ao processar', 5000);
notificationsService.info('Informação', 4000);
notificationsService.warning('Aviso!', 4000);

notificationsService.loading('Processando...');  // Sem timeout

notificationsService.confirm('Tem certeza?', 
  () => { /* confirmou */ },
  () => { /* cancelou */ }
);

notificationsService.clearAll();  // Remove tudo
```

---

## 📌 Exemplo de Uso Completo

```javascript
// No login.html
async function fazerLogin() {
  // Validação
  const email = document.getElementById('email').value;
  if (!validators.email(email)) {
    notificationsService.error('Email inválido!');
    return;
  }

  // API
  const result = await authAPI.login(email, senha);
  if (result.error) {
    notificationsService.error(result.error);
    return;
  }

  // Storage
  storageService.set('usuario', result.data);

  // Notifica sucesso
  notificationsService.success('Bem-vindo!');
  
  // Redireciona
  setTimeout(() => {
    window.location.href = '/pages/dashboard';
  }, 1500);
}
```

---

## 🚀 Fluxo de Requisição

```
usuario clica "Entrar"
    ↓
JavaScript valida (validators)
    ↓
authAPI.login(email, senha)
    ↓
apiClient.post('/auth/login', {email, senha})
    ↓
Adiciona header: Authorization: Bearer {token}
    ↓
Backend responde
    ↓
Se erro: notificationsService.error()
Se sucesso: 
  - storageService.set() salva token
  - notificationsService.success() mostra sucesso
  - Redireciona para próxima página
```

---

## ⚠️ Tratamento de Erros Automáticos

Se token expirar (401):
- `apiClient` limpa o token automaticamente
- Redireciona para `/pages/login`
- `errorHandler` registra o evento

---

## 📋 Checklist de Integração

Para cada página HTML:

- [ ] Adicionar `<script src="/js/modules.js"></script>` no head
- [ ] Aguardar evento `modulesLoaded` ou verificar `window.authAPI`
- [ ] Usar APIs centralizadas (não fetch direto)
- [ ] Validar inputs com `validators`
- [ ] Armazenar dados com `storageService`
- [ ] Mostrar notificações com `notificationsService`
- [ ] Formatar exibição com `formatters`
- [ ] Registrar erros com `errorHandler`

---

**Próximos passos**: Integrar dashboard, perfil, ofertas e demais telas com essas APIs.
