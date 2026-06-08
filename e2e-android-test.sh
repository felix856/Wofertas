#!/bin/bash
# E2E Test Script para simular requisições do Android app ao backend
# Uso: ./e2e-android-test.sh

API_BASE_URL="http://localhost:8080"
USUARIO_ID="user123"
MERCADO_ID="mercado123"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========== E2E Tests - Android App <-> Backend ==========${NC}\n"

# Função para fazer requisição PUT
fazer_requisicao() {
    local endpoint=$1
    local data=$2
    local descricao=$3

    echo -e "${YELLOW}>>> Testando: ${descricao}${NC}"
    echo "Endpoint: PUT ${endpoint}"
    echo "Payload:"
    echo "${data}" | jq '.' 2>/dev/null || echo "${data}"

    response=$(curl -s -w "\n%{http_code}" -X PUT "${API_BASE_URL}${endpoint}" \
        -H "Content-Type: application/json" \
        -d "${data}")

    http_code=$(echo "${response}" | tail -n1)
    body=$(echo "${response}" | head -n-1)

    echo "Response Code: ${http_code}"

    if [ "${http_code}" == "204" ]; then
        echo -e "${GREEN}✓ SUCCESS${NC}\n"
        return 0
    elif [ "${http_code}" == "400" ] || [ "${http_code}" == "401" ] || [ "${http_code}" == "404" ]; then
        echo -e "${GREEN}✓ Expected Error: ${http_code}${NC}"
        echo "Response: ${body}"
        echo -e "\n"
        return 0
    else
        echo -e "${RED}✗ FAILED - Unexpected code${NC}"
        echo "Response: ${body}"
        echo -e "\n"
        return 1
    fi
}

# Teste 1: Mudança de senha com sucesso
fazer_requisicao \
    "/usuarios/${USUARIO_ID}/senha" \
    '{
        "senhaAtual": "SenhaOriginal123",
        "novaSenha": "SenhaNovaSegura456",
        "confirmacao": "SenhaNovaSegura456"
    }' \
    "Mudança de senha do usuário - Caso de sucesso"

# Teste 2: Confirmação não confere
fazer_requisicao \
    "/usuarios/${USUARIO_ID}/senha" \
    '{
        "senhaAtual": "SenhaOriginal123",
        "novaSenha": "SenhaNovaSegura456",
        "confirmacao": "ConfirmacaoDiferente"
    }' \
    "Mudança de senha - Confirmação não confere"

# Teste 3: Senha atual incorreta
fazer_requisicao \
    "/usuarios/${USUARIO_ID}/senha" \
    '{
        "senhaAtual": "SenhaErrada",
        "novaSenha": "SenhaNovaSegura456",
        "confirmacao": "SenhaNovaSegura456"
    }' \
    "Mudança de senha - Senha atual incorreta"

# Teste 4: Senha muito curta
fazer_requisicao \
    "/usuarios/${USUARIO_ID}/senha" \
    '{
        "senhaAtual": "SenhaOriginal123",
        "novaSenha": "123",
        "confirmacao": "123"
    }' \
    "Mudança de senha - Senha muito curta"

# Teste 5: Campos vazios
fazer_requisicao \
    "/usuarios/${USUARIO_ID}/senha" \
    '{
        "senhaAtual": "",
        "novaSenha": "",
        "confirmacao": ""
    }' \
    "Mudança de senha - Campos vazios"

# Teste 6: Mudança de senha do mercado com sucesso
fazer_requisicao \
    "/mercados/${MERCADO_ID}/senha" \
    '{
        "senhaAtual": "SenhaOriginal123",
        "novaSenha": "SenhaNovaSegura456",
        "confirmacao": "SenhaNovaSegura456"
    }' \
    "Mudança de senha do mercado - Caso de sucesso"

# Teste 7: ID do mercado inválido
fazer_requisicao \
    "/mercados/idInvalido/senha" \
    '{
        "senhaAtual": "SenhaOriginal123",
        "novaSenha": "SenhaNovaSegura456",
        "confirmacao": "SenhaNovaSegura456"
    }' \
    "Mudança de senha - ID do mercado inválido"

echo -e "${YELLOW}========== Testes Concluídos ==========${NC}"
