# Wofertas Python RPA

## Estrutura

```
rpa/
├── main.py                   # Classe principal WofertasRPA
├── api_client.py             # Cliente HTTP para API REST (retry, JWT, cache)
├── ia_coach.py               # IA Coach (Claude API)
├── analise_estrategica.py    # Análise dados + Recomendações
├── requirements.txt          # Dependências Python
├── .env.example             # Variáveis de ambiente (exemplo)
└── README.md                # Este arquivo
```

## Instalação

```bash
# 1. Criar venv
python -m venv venv
source venv/Scripts/activate  # Windows: venv\Scripts\activate

# 2. Instalar dependências
pip install -r requirements.txt

# 3. Configurar .env
cp .env.example .env
# Editar .env com suas credenciais (Anthropic API Key, etc)
```

## Funcionalidades

### 1. **Dashboard Analytics**
Extrai dados de negócio e os processa:

```python
from rpa.main import WofertasRPA

rpa = WofertasRPA()
rpa.authenticate('user@example.com', 'password')
analytics = rpa.get_dashboard_analytics()
# Retorna: {vendas_mes, ofertas_totais, taxa_conversao, usuarios_ativos}
```

### 2. **IA Coach**
Guia usuários leigos com respostas inteligentes em português:

```python
resposta = rpa.run_ai_coach("Como faço para vender uma oferta?")
# Claude retorna: "1. Clique em 'Publicar Oferta'...\n2. Preencha os dados...\n..."
```

### 3. **Recomendações Estratégicas**
Análise automática de negócio com sugestões de ações:

```python
recomendacoes = rpa.generate_strategic_recommendations()
# Retorna:
# [
#   {titulo: "Melhorar Descrição", impacto: "alto", acao: "..."},
#   {titulo: "Expandir Catálogo", impacto: "alto", acao: "..."}
# ]
```

## TODO - Próximas Implementações

### Curto Prazo (1-2 semanas)
- [ ] Integração com APScheduler (agendamento de tarefas)
- [ ] Cache local com SQLite (alternativa a Redis)
- [ ] Testes unitários (pytest)
- [ ] Endpoint FastAPI para expor RPA como serviço

### Médio Prazo (2-4 semanas)
- [ ] Webhook de dashboard em tempo real
- [ ] Integração com sistema de notificações (WhatsApp/Email)
- [ ] Dashboard visual com dados da RPA (Grafana/Webhook)
- [ ] Análise de padrões de vendas (ML básico)

### Longo Prazo (1+ mês)
- [ ] Containerizar RPA (Docker)
- [ ] Deploy em Azure Functions ou AWS Lambda
- [ ] Integração com Kotlin mobile (push notifications)
- [ ] Sistema de recomendações de preço com ML

## Exemplo de Uso Completo

```python
import os
from rpa.main import WofertasRPA
from rpa.ia_coach import IACoach
from rpa.analise_estrategica import AnalisadorEstrategico

# 1. Autenticar
rpa = WofertasRPA()
rpa.authenticate(
    os.getenv('WOFERTAS_USER_EMAIL'),
    os.getenv('WOFERTAS_USER_PASSWORD')
)

# 2. Extrair analytics
analytics = rpa.get_dashboard_analytics()
print(f"Vendas este mês: R$ {analytics['vendas_mes']:,.2f}")

# 3. Gerar recomendações
recomendacoes = rpa.generate_strategic_recommendations()
for rec in recomendacoes:
    print(f"✓ {rec['titulo']} ({rec['impacto']})")
    print(f"  Ação: {rec['acao']}")

# 4. Responder pergunta do usuário
coach = IACoach(os.getenv('ANTHROPIC_API_KEY'))
resposta = coach.answer_question(
    "Como aumento minha taxa de conversão?",
    dashboard_context=analytics
)
print(resposta)
```

## Configuração em Produção

### Variáveis de Ambiente Obrigatórias

```bash
export WOFERTAS_API_URL=https://wofertas.com.br/api
export MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/wofertas
export ANTHROPIC_API_KEY=sk-ant-...
```

### Com Docker

```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY rpa/requirements.txt .
RUN pip install -r requirements.txt

COPY rpa/ .

CMD ["python", "main.py"]
```

```bash
docker build -t wofertas-rpa .
docker run -e ANTHROPIC_API_KEY=$ANTHROPIC_API_KEY wofertas-rpa
```

## Logs

```
# Arquivo: wofertas_rpa.log
2026-04-16 10:30:15 - rpa.main - INFO - Wofertas RPA inicializada com sucesso
2026-04-16 10:30:16 - rpa.api_client - DEBUG - [Attempt 1] GET /analytics/dashboard
2026-04-16 10:30:17 - rpa.main - INFO - Dados de analytics extraídos
2026-04-16 10:30:18 - rpa.ia_coach - INFO - Pergunta do usuário: Como aumento...
```

## Testes

```bash
pytest test/ -v --cov=rpa
```

## Suporte

Para problemas:
1. Verifique `.env` está configurado
2. Veja logs em `wofertas_rpa.log`
3. Teste autenticação: `python -m rpa.api_client`
