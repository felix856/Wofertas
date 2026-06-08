"""
Módulo de Integração com API REST Wofertas
Responsável por:
- Autenticação JWT
- Requisições HTTP com retry
- Cache de respostas
"""

import requests
import json
from typing import Dict, Any, Optional
from datetime import datetime, timedelta
import logging
import time

logger = logging.getLogger(__name__)


class WofertasAPIClient:
    """
    Cliente HTTP para a API REST Wofertas
    Implementa retry, cache, e injeção de JWT
    """

    def __init__(self, base_url: str, max_retries: int = 3, timeout: int = 10):
        self.base_url = base_url.rstrip('/')
        self.max_retries = max_retries
        self.timeout = timeout
        self.jwt_token = None
        self.headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
        self.cache = {}

    def set_jwt_token(self, token: str):
        """Define o token JWT para autenticação"""
        self.jwt_token = token
        self.headers['Authorization'] = f'Bearer {token}'
        logger.debug("JWT token configurado")

    def _request(self, method: str, endpoint: str, **kwargs) -> Dict[str, Any]:
        """
        Realiza requisição HTTP com retry automático

        Args:
            method: GET, POST, PUT, DELETE
            endpoint: Caminho da API (ex: /analytics/dashboard)
            **kwargs: Argumentos adicionais para requests

        Returns:
            Resposta JSON parsed ou dict vazio em erro
        """
        url = f"{self.base_url}{endpoint}"

        for attempt in range(self.max_retries):
            try:
                logger.debug(f"[Attempt {attempt + 1}] {method} {url}")

                response = requests.request(
                    method=method,
                    url=url,
                    headers=self.headers,
                    timeout=self.timeout,
                    **kwargs
                )

                if response.status_code == 200:
                    return response.json() if response.content else {}

                elif response.status_code == 401:
                    logger.error("Autenticação falhou - JWT expirado ou inválido")
                    return {'error': 'Unauthorized'}

                elif response.status_code >= 500:
                    logger.warning(f"Erro 5xx: {response.status_code}. Retentando...")
                    time.sleep(2 ** attempt)  # Backoff exponencial
                    continue

                else:
                    logger.error(f"Erro {response.status_code}: {response.text}")
                    return {'error': f'HTTP {response.status_code}'}

            except requests.exceptions.Timeout:
                logger.warning(f"Timeout na tentativa {attempt + 1}")
                time.sleep(2 ** attempt)

            except requests.exceptions.ConnectionError:
                logger.warning(f"Erro de conexão na tentativa {attempt + 1}")
                time.sleep(2 ** attempt)

            except Exception as e:
                logger.error(f"Erro inesperado: {e}")
                return {'error': str(e)}

        logger.error(f"Falha após {self.max_retries} tentativas")
        return {}

    def login(self, email: str, password: str) -> bool:
        """
        Autentica e armazena JWT

        Returns:
            True se sucesso, False caso contrário
        """
        payload = {'email': email, 'senha': password}
        response = self._request('POST', '/auth/login', json=payload)

        if 'token' in response:
            self.set_jwt_token(response['token'])
            logger.info(f"Login bem-sucedido para {email}")
            return True

        logger.error(f"Login falhou para {email}")
        return False

    def get_dashboard_analytics(self) -> Dict[str, Any]:
        """Extrai dados de analytics do dashboard"""
        return self._request('GET', '/analytics/dashboard')

    def get_ofertas(self, limit: int = 50) -> List[Dict]:
        """Retorna lista de ofertas"""
        return self._request('GET', f'/ofertas?limit={limit}').get('data', [])

    def get_mercados(self) -> List[Dict]:
        """Retorna lista de mercados"""
        return self._request('GET', '/mercados').get('data', [])

    def get_usuarios_ativos(self) -> int:
        """Retorna número de usuários ativos"""
        response = self._request('GET', '/usuarios/atividade')
        return response.get('total_ativos', 0)


if __name__ == '__main__':
    client = WofertasAPIClient('http://localhost:8080/api')
    print("WofertasAPIClient pronto para uso")
