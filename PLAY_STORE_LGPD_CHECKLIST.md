# Wofertas - Checklist Play Store e LGPD

Atualizado em: 2026-06-22

## URLs para Play Console

- Politica de privacidade: `https://wofertas.vercel.app/privacy-policy.html`
- Exclusao de conta: `https://wofertas.vercel.app/excluir-conta.html`
- Termos de uso: `https://wofertas.vercel.app/termos.html`

## Dados que o app trata

- Conta: nome, e-mail, senha com hash, tipo de perfil.
- Mercado: CNPJ, telefone, endereco, logo e coordenadas do mercado.
- Localizacao do cliente: usada para ofertas proximas quando a permissao e concedida.
- Interacoes: visualizacoes, curtidas, favoritos, carrinho/lista de compras e cliques.
- Notificacoes: token FCM quando o usuario permite notificacoes.
- Imagens/arquivos: fotos/logos e encartes enviados pelo mercado.

## Play Console - Data Safety

Declare que o app:

- Coleta dados pessoais de conta e contato.
- Coleta localizacao para recurso de ofertas proximas/mapa.
- Coleta atividade no app para analytics, favoritos, carrinho e recomendacoes.
- Usa criptografia em transito em producao.
- Oferece mecanismo de solicitacao de exclusao de dados dentro do app e pela web.
- Usa terceiros/SDKs para mapas, rede, imagens, OCR/notificacoes conforme dependencias do app.

## Implementado no codigo

- Endpoints LGPD:
  - `GET /api/privacy/legal`
  - `GET /api/privacy/me/export`
  - `POST /api/privacy/me/consent`
  - `POST /api/privacy/me/deletion-request`
  - `POST /api/privacy/public/deletion-request`
- Registro de solicitacoes em `data_privacy_requests`.
- Campos de aceite/versao legal em `usuario` e `mercado`.
- Politica, termos e exclusao em `view/`.
- App Android com botao de privacidade e solicitacao de exclusao em Perfil.
- Token de sessao em `EncryptedSharedPreferences`.
- Release Android sem cleartext HTTP, sem backup e com R8/shrink habilitados.

## Antes de publicar

- Trocar `privacidade@wofertas.com.br` pelo e-mail real de atendimento.
- Garantir que as paginas `view/` estejam publicadas no Vercel.
- Conferir o formulario Data Safety no Play Console com as praticas reais do app.
- Gerar AAB assinado para Play Store; APK unsigned nao deve ser enviado em producao.
- Considerar trocar `applicationId` de `com.example.wofertas` para um identificador definitivo antes da primeira publicacao.
