"""
Módulo de Análise Estratégica
Analisa dados de negócio e gera recomendações inteligentes
"""

import logging
from typing import List, Dict, Any
from dataclasses import dataclass
from datetime import datetime, timedelta

logger = logging.getLogger(__name__)


@dataclass
class Recomendacao:
    """Estrutura de uma recomendação estratégica"""
    titulo: str
    descricao: str
    impacto: str  # alto, medio, baixo
    esforco: str  # facil, medio, dificil
    acao: str
    metrica_alvo: str


class AnalisadorEstrategico:
    """
    Analisa métricas de negócio e gera recomendações baseadas em regras
    """

    def __init__(self):
        self.regras = self._carregar_regras()
        logger.info("AnalisadorEstrategico inicializado")

    def analisar_dashboard(self, metricas: Dict[str, Any]) -> List[Recomendacao]:
        """
        Analisa métricas e retorna recomendações priorizadas

        Args:
            metricas: {
                'vendas_mes': float,
                'ofertas_totais': int,
                'taxa_conversao': float,
                'usuarios_ativos': int,
                'receita_media': float
            }

        Returns:
            Lista de Recomendacao ordenadas por impacto
        """
        logger.info("Analisando dashboard...")
        recomendacoes = []

        # Regra 1: Taxa de conversão baixa
        if metricas.get('taxa_conversao', 0) < 0.03:
            recomendacoes.append(
                Recomendacao(
                    titulo="Melhorar Descrição de Ofertas",
                    descricao="Taxa de conversão abaixo de 3%. Adicione mais detalhes e imagens.",
                    impacto="alto",
                    esforco="facil",
                    acao="Edite suas 5 melhores ofertas com descrições mais detalhadas",
                    metrica_alvo="taxa_conversao > 5%"
                )
            )

        # Regra 2: Poucas ofertas ativas
        if metricas.get('ofertas_totais', 0) < 10:
            recomendacoes.append(
                Recomendacao(
                    titulo="Expandir Catálogo de Ofertas",
                    descricao=f"Apenas {metricas.get('ofertas_totais', 0)} ofertas. Vendedores com 20+ têm 4x mais vendas.",
                    impacto="alto",
                    esforco="medio",
                    acao="Crie pelo menos 10 novas ofertas com categorias variadas",
                    metrica_alvo="ofertas_totais >= 20"
                )
            )

        # Regra 3: Vendas muito baixas
        vendas_mes = metricas.get('vendas_mes', 0)
        if vendas_mes < 500:
            recomendacoes.append(
                Recomendacao(
                    titulo="Ajustar Preços de Forma Estratégica",
                    descricao=f"Vendas de R${vendas_mes:.2f} neste mês. Considere promoções pontuais.",
                    impacto="medio",
                    esforco="facil",
                    acao="Procure ofertas similares e ajuste seu preço competitivamente",
                    metrica_alvo="vendas_mes > R$ 2000"
                )
            )

        # Regra 4: Poucos usuários ativos
        if metricas.get('usuarios_ativos', 0) < 50:
            recomendacoes.append(
                Recomendacao(
                    titulo="Aumentar Visibilidade no Mercado",
                    descricao="Pouca atividade de usuários. Promova suas melhores ofertas.",
                    impacto="medio",
                    esforco="medio",
                    acao="Use tags relevantes e destaque ofertas premium",
                    metrica_alvo="usuarios_ativos > 200"
                )
            )

        # Ordenar por impacto (alto > medio > baixo)
        ordem_impacto = {'alto': 0, 'medio': 1, 'baixo': 2}
        recomendacoes.sort(key=lambda r: ordem_impacto.get(r.impacto, 3))

        logger.info(f"Geradas {len(recomendacoes)} recomendações")
        return recomendacoes

    def formatar_para_json(self, recomendacoes: List[Recomendacao]) -> List[Dict[str, str]]:
        """Converte recomendações para formato JSON"""
        return [
            {
                'titulo': r.titulo,
                'descricao': r.descricao,
                'impacto': r.impacto,
                'esforco': r.esforco,
                'acao': r.acao,
                'metrica_alvo': r.metrica_alvo
            }
            for r in recomendacoes
        ]

    def _carregar_regras(self) -> List[Dict]:
        """Carrega regras de negócio (pode vir de arquivo JSON no futuro)"""
        return [
            {'nome': 'conversao_baixa', 'threshold': 0.03},
            {'nome': 'ofertas_insuficientes', 'threshold': 10},
            {'nome': 'vendas_baixas', 'threshold': 500},
            {'nome': 'usuarios_poucos', 'threshold': 50},
        ]


if __name__ == '__main__':
    analisador = AnalisadorEstrategico()

    # Teste com dados simulados
    metricas_teste = {
        'vendas_mes': 250,
        'ofertas_totais': 5,
        'taxa_conversao': 0.02,
        'usuarios_ativos': 30,
        'receita_media': 85.50
    }

    recomendacoes = analisador.analisar_dashboard(metricas_teste)
    json_output = analisador.formatar_para_json(recomendacoes)

    import json
    print(json.dumps(json_output, indent=2, ensure_ascii=False))
