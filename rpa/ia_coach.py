"""
Módulo de IA Coach
Guia usuários leigos pelo sistema Wofertas usando Claude API
Enriquece perguntas com contexto do dashboard
"""

import anthropic
import logging
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)


class IACoach:
    """
    Coach inteligente que usa Claude para guiar usuários
    Entende contexto do negócio e responde em português
    """

    SYSTEM_PROMPT = """Você é um assistant especializado em plataforma de marketplace Wofertas.
Sua função é guiar usuários leigos de forma simples e objetiva.

Contexto sobre Wofertas:
- É uma plataforma de compra e venda de ofertas
- Usuários podem ser Compradores (exploram ofertas) ou Vendedores (criam ofertas)
- Cada oferta tem: nome, preço, descrição, imagens
- Favoritos permitem salvar ofertas de interesse
- Dashboard mostra histórico de compras, estatísticas de vendas

Regras de resposta:
1. Use linguagem simples, evite jargão técnico
2. Divida respostas em passos numerados quando apropriado
3. Responda apenas sobre Wofertas, redirecione outras perguntas
4. Sempre responda em português (PT-BR)
5. Se não souber, indique que entrará em contato com suporte

Exemplos de perguntas esperadas:
- "Como faço para vender uma oferta?"
- "Como adiciono itens aos favoritos?"
- "Como vejo meu histórico de compras?"
- "Qual é a taxa de comissão?"
"""

    def __init__(self, api_key: str):
        """Inicializa com chave da API Anthropic"""
        self.client = anthropic.Anthropic(api_key=api_key)
        self.model = "claude-3-5-sonnet-20241022"
        logger.info("IACoach inicializado")

    def answer_question(
        self,
        question: str,
        dashboard_context: Optional[Dict[str, Any]] = None
    ) -> str:
        """
        Responde pergunta do usuário com contexto do dashboard

        Args:
            question: Pergunta do usuário
            dashboard_context: Contexto de analytics {ofertas_totais, receita_mes, etc}

        Returns:
            Resposta gerada pelo Claude
        """
        logger.info(f"Pergunta do usuário: {question[:50]}...")

        # Enriquece contexto com dados do dashboard
        user_message = question
        if dashboard_context:
            context_str = self._format_dashboard_context(dashboard_context)
            user_message = f"{context_str}\n\nPergunta do usuário: {question}"

        try:
            response = self.client.messages.create(
                model=self.model,
                max_tokens=1024,
                system=self.SYSTEM_PROMPT,
                messages=[
                    {"role": "user", "content": user_message}
                ]
            )

            answer = response.content[0].text
            logger.info("Resposta gerada com sucesso")
            return answer

        except anthropic.APIError as e:
            logger.error(f"Erro na API Anthropic: {e}")
            return "Desculpe, não consegui processar sua pergunta. Tente novamente em instantes."

    def _format_dashboard_context(self, context: Dict[str, Any]) -> str:
        """Formata contexto do dashboard para incluir na pergunta"""
        lines = ["[CONTEXTO DO DASHBOARD]"]

        if 'vendas_mes' in context:
            lines.append(f"- Vendas neste mês: R$ {context['vendas_mes']:,.2f}")

        if 'ofertas_totais' in context:
            lines.append(f"- Total de ofertas: {context['ofertas_totais']}")

        if 'usuarios_ativos' in context:
            lines.append(f"- Usuários ativos: {context['usuarios_ativos']}")

        if 'taxa_conversao' in context:
            lines.append(f"- Taxa de conversão: {context['taxa_conversao']:.1%}")

        return "\n".join(lines)


if __name__ == '__main__':
    import os
    api_key = os.getenv('ANTHROPIC_API_KEY')
    coach = IACoach(api_key)

    # Teste
    answer = coach.answer_question("Como faço para vender uma oferta?")
    print(answer)
