@echo off
REM E2E Test Script para simular requisições do Android app ao backend
REM Uso: e2e-android-test.bat

setlocal enabledelayedexpansion

set "API_BASE_URL=http://localhost:8080"
set "USUARIO_ID=user123"
set "MERCADO_ID=mercado123"

echo.
echo ========== E2E Tests - Android App ^<-^> Backend ==========
echo.

REM Teste 1: Mudança de senha com sucesso
echo [TESTE 1] Mudanca de senha do usuario - Caso de sucesso
curl -X PUT "%API_BASE_URL%/usuarios/%USUARIO_ID%/senha" ^
    -H "Content-Type: application/json" ^
    -d "{\"senhaAtual\": \"SenhaOriginal123\", \"novaSenha\": \"SenhaNovaSegura456\", \"confirmacao\": \"SenhaNovaSegura456\"}"
echo.
echo.

REM Teste 2: Confirmação não confere
echo [TESTE 2] Mudanca de senha - Confirmacao nao confere
curl -X PUT "%API_BASE_URL%/usuarios/%USUARIO_ID%/senha" ^
    -H "Content-Type: application/json" ^
    -d "{\"senhaAtual\": \"SenhaOriginal123\", \"novaSenha\": \"SenhaNovaSegura456\", \"confirmacao\": \"ConfirmacaoDiferente\"}"
echo.
echo.

REM Teste 3: Senha atual incorreta
echo [TESTE 3] Mudanca de senha - Senha atual incorreta
curl -X PUT "%API_BASE_URL%/usuarios/%USUARIO_ID%/senha" ^
    -H "Content-Type: application/json" ^
    -d "{\"senhaAtual\": \"SenhaErrada\", \"novaSenha\": \"SenhaNovaSegura456\", \"confirmacao\": \"SenhaNovaSegura456\"}"
echo.
echo.

REM Teste 4: Senha muito curta
echo [TESTE 4] Mudanca de senha - Senha muito curta
curl -X PUT "%API_BASE_URL%/usuarios/%USUARIO_ID%/senha" ^
    -H "Content-Type: application/json" ^
    -d "{\"senhaAtual\": \"SenhaOriginal123\", \"novaSenha\": \"123\", \"confirmacao\": \"123\"}"
echo.
echo.

REM Teste 5: Campos vazios
echo [TESTE 5] Mudanca de senha - Campos vazios
curl -X PUT "%API_BASE_URL%/usuarios/%USUARIO_ID%/senha" ^
    -H "Content-Type: application/json" ^
    -d "{\"senhaAtual\": \"\", \"novaSenha\": \"\", \"confirmacao\": \"\"}"
echo.
echo.

REM Teste 6: Mudança de senha do mercado com sucesso
echo [TESTE 6] Mudanca de senha do mercado - Caso de sucesso
curl -X PUT "%API_BASE_URL%/mercados/%MERCADO_ID%/senha" ^
    -H "Content-Type: application/json" ^
    -d "{\"senhaAtual\": \"SenhaOriginal123\", \"novaSenha\": \"SenhaNovaSegura456\", \"confirmacao\": \"SenhaNovaSegura456\"}"
echo.
echo.

REM Teste 7: ID do mercado inválido
echo [TESTE 7] Mudanca de senha - ID do mercado invalido
curl -X PUT "%API_BASE_URL%/mercados/idInvalido/senha" ^
    -H "Content-Type: application/json" ^
    -d "{\"senhaAtual\": \"SenhaOriginal123\", \"novaSenha\": \"SenhaNovaSegura456\", \"confirmacao\": \"SenhaNovaSegura456\"}"
echo.
echo.

echo ========== Testes Concluidos ==========
echo.

endlocal
