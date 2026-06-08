package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.DashboardAnalyticsDTO;
import com.example.demo.dto.InsightEstrategicoDTO;
import com.example.demo.dto.OfertaAnalyticsDTO;
import com.example.demo.model.Curtida;
import com.example.demo.model.Favorito;
import com.example.demo.model.ItemCarrinho;
import com.example.demo.model.Oferta;
import com.example.demo.repository.CurtidaRepository;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.ItemCarrinhoRepository;
import com.example.demo.repository.OfertaRepository;
import com.example.demo.repository.VisualizacaoRepository;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.model.Mercado;
import com.example.demo.dto.MercadoRankingDTO;

@Service
public class AnalyticsService {

    @Autowired private OfertaRepository ofertaRepository;
    @Autowired private CurtidaRepository curtidaRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private VisualizacaoRepository visualizacaoRepository;
    @Autowired private ItemCarrinhoRepository itemCarrinhoRepository;
    @Autowired private MercadoRepository mercadoRepository;

    public DashboardAnalyticsDTO gerarDashboardMercado(String mercadoId) {
        // 1. Buscar todas as ofertas do mercado
        List<Oferta> ofertas = ofertaRepository.findByMercadoId(mercadoId);
        long totalFavoritos = favoritoRepository.countByIdMercado(mercadoId);

        if (ofertas.isEmpty()) {
            // Retornar dashboard vazio se sem ofertas
            return new DashboardAnalyticsDTO(
                0, 0, totalFavoritos, 0, 0, 0.0, 0.0,
                List.of(), List.of(), List.of(),
                Map.of(), Map.of(), Map.of(),
                new InsightEstrategicoDTO("N/A", 0.0, "Publique suas primeiras ofertas!", (int) totalFavoritos, "Sem dados suficientes")
            );
        }

        // 2. Coletar dados de analytics (SEM EXPOR PII)
        long totalVisualizacoes = 0;
        long totalCurtidas = 0;
        long totalItensCarrinho = 0;

        List<OfertaAnalyticsDTO> encartesComDados = new ArrayList<>();
        Map<String, Long> produtosComMaiorCurtidas = new HashMap<>();
        Map<String, Long> produtosComMaiorCarrinho = new HashMap<>();
        Set<String> usuariosUnicos = new HashSet<>();

        for (Oferta oferta : ofertas) {
            long curtidas = curtidaRepository.countByIdOferta(oferta.getId());
            long visualizacoes = visualizacaoRepository.countByIdOferta(oferta.getId());
            long itensCarrinho = itemCarrinhoRepository.countByIdOferta(oferta.getId());

            totalVisualizacoes += visualizacoes;
            totalCurtidas += curtidas;
            totalItensCarrinho += itensCarrinho;

            // Contar usuários únicos que interagiram (SEM ARMAZENAR DADOS PESSOAIS)
            usuariosUnicos.addAll(
                curtidaRepository.findByIdOferta(oferta.getId())
                    .stream()
                    .map(Curtida::getIdUsuario)
                    .collect(Collectors.toSet())
            );
            usuariosUnicos.addAll(
                itemCarrinhoRepository.findByIdOferta(oferta.getId())
                    .stream()
                    .map(ItemCarrinho::getIdUsuario)
                    .collect(Collectors.toSet())
            );
            usuariosUnicos.addAll(
                favoritoRepository.findByIdMercado(mercadoId)
                    .stream()
                    .map(Favorito::getIdUsuario)
                    .collect(Collectors.toSet())
            );

            // Calculate engagement
            double engajamento = visualizacoes > 0 ? (curtidas + itensCarrinho) * 100.0 / visualizacoes : 0;

            OfertaAnalyticsDTO dto = new OfertaAnalyticsDTO(
                oferta.getId(),
                oferta.getNome(),
                oferta.getStatus(),
                oferta.getImagemOferta(),
                curtidas,
                visualizacoes,
                itensCarrinho,
                Math.round(engajamento * 100.0) / 100.0
            );

            encartesComDados.add(dto);

            // Agregar preferências (nome do produto -> quantidade, SEM DADOS DE QUEM CURTIU)
            produtosComMaiorCurtidas.merge(oferta.getNome(), curtidas, (a, b) -> a + b);
            produtosComMaiorCarrinho.merge(oferta.getNome(), itensCarrinho, (a, b) -> a + b);
        }

        // 3. Ranking de encartes (top 5 por engajamento)
        List<OfertaAnalyticsDTO> encartesRanking = encartesComDados.stream()
            .sorted((a, b) -> Double.compare(b.getEngajamento(), a.getEngajamento()))
            .limit(5)
            .collect(Collectors.toList());

        // 4. Top 5 por curtidas
        List<OfertaAnalyticsDTO> encartesComMaiorCurtidas = encartesComDados.stream()
            .sorted((a, b) -> Long.compare(b.getCurtidas(), a.getCurtidas()))
            .limit(5)
            .collect(Collectors.toList());

        // 5. Top 5 por carrinho
        List<OfertaAnalyticsDTO> encartesComMaiorCarrinho = encartesComDados.stream()
            .sorted((a, b) -> Long.compare(b.getItensCarrinho(), a.getItensCarrinho()))
            .limit(5)
            .collect(Collectors.toList());

        // 6. Produtos mais curtidos e mais adicionados (agregado, sem PII)
        Map<String, Long> topProdutosCurtidas = produtosComMaiorCurtidas.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e, a) -> e, LinkedHashMap::new));

        Map<String, Long> topProdutosCarrinho = produtosComMaiorCarrinho.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e, a) -> e, LinkedHashMap::new));

        // 7. Distribuição por origem (visualizações)
        Map<String, Long> vizOrigem = new HashMap<>();
        for (Oferta oferta : ofertas) {
            visualizacaoRepository.findByIdOferta(oferta.getId()).forEach(viz -> {
                vizOrigem.merge(viz.getOrigem() != null ? viz.getOrigem() : "DESCONHECIDA", 1L, (a, b) -> a + b);
            });
        }

        // 8. Calcular taxas de conversão
        double taxaConversaoCurtidas = totalVisualizacoes > 0 ? (totalCurtidas * 100.0 / totalVisualizacoes) : 0.0;
        double taxaConversaoCarrinho = totalVisualizacoes > 0 ? (totalItensCarrinho * 100.0 / totalVisualizacoes) : 0.0;

        // 9. Gerar insights estratégicos
        InsightEstrategicoDTO insight = gerarInsights(
            encartesRanking,
            encartesComDados,
            taxaConversaoCurtidas,
            taxaConversaoCarrinho,
            usuariosUnicos.size()
        );

        return new DashboardAnalyticsDTO(
            totalVisualizacoes,
            totalCurtidas,
            totalFavoritos,
            totalItensCarrinho,
            ofertas.size(),
            Math.round(taxaConversaoCurtidas * 100.0) / 100.0,
            Math.round(taxaConversaoCarrinho * 100.0) / 100.0,
            encartesRanking,
            encartesComMaiorCurtidas,
            encartesComMaiorCarrinho,
            topProdutosCurtidas,
            topProdutosCarrinho,
            vizOrigem,
            insight
        );
    }

    private InsightEstrategicoDTO gerarInsights(
        List<OfertaAnalyticsDTO> ranking,
        List<OfertaAnalyticsDTO> todosEncartes,
        double taxaCurtidas,
        double taxaCarrinho,
        int clientesAtivos
    ) {
        // Nome do melhor encarte
        String melhorEncarte = ranking.isEmpty() ? "N/A" : ranking.get(0).getNome();

        // Engajamento médio
        double engajamentoMedio = todosEncartes.stream()
            .mapToDouble(OfertaAnalyticsDTO::getEngajamento)
            .average()
            .orElse(0.0);
        engajamentoMedio = Math.round(engajamentoMedio * 100.0) / 100.0;

        // Recomendação estratégica baseada nos dados (SEM MOSTRAR DADOS DOS CLIENTES)
        String recomendacao = gerarRecomendacao(taxaCurtidas, taxaCarrinho, engajamentoMedio, todosEncartes.size());

        // Tendência identificada
        String tendencia = identificarTendencia(ranking);

        return new InsightEstrategicoDTO(
            melhorEncarte,
            engajamentoMedio,
            recomendacao,
            clientesAtivos,
            tendencia
        );
    }

    private String gerarRecomendacao(double taxaCurtidas, double taxaCarrinho, double engajamentoMedio, int totalEncartes) {
        if (totalEncartes == 0) {
            return "Publique suas primeiras ofertas para comecar a gerar dados de clientes.";
        }

        if (taxaCarrinho > 5.0) {
            return "Excelente: seus encartes tem alta conversao. Mantenha a qualidade e aumente a frequencia de publicacoes.";
        } else if (taxaCurtidas > 10.0) {
            return "Seus encartes geram interesse, mas precisam incentivar melhor a compra. Teste chamadas mais claras.";
        } else if (engajamentoMedio < 2.0) {
            return "Engajamento baixo. Revise a qualidade das imagens e descricoes dos seus encartes.";
        } else {
            return "Performance na media. Teste diferentes tipos de ofertas e horarios de publicacao.";
        }
    }

    private String identificarTendencia(List<OfertaAnalyticsDTO> ranking) {
        if (ranking.isEmpty()) {
            return "Sem dados suficientes";
        }

        long curtidaTotal = ranking.stream().mapToLong(OfertaAnalyticsDTO::getCurtidas).sum();
        long carrinhoTotal = ranking.stream().mapToLong(OfertaAnalyticsDTO::getItensCarrinho).sum();

        if (carrinhoTotal > curtidaTotal * 2) {
            return "Clientes com alto interesse em compra - aumente inventario dos top 3 produtos";
        } else if (curtidaTotal > carrinhoTotal * 3) {
            return "Alto interesse mas baixa conversao - ajuste precos ou ofereca descontos";
        } else {
            return "Balanceamento saudavel entre visualizacoes e compras";
        }
    }

    public List<MercadoRankingDTO> obterRankingMercados() {
        List<Mercado> mercados = mercadoRepository.findAll();
        List<MercadoRankingDTO> rankingTemp = new ArrayList<>();

        for (Mercado m : mercados) {
            List<Oferta> ofertas = ofertaRepository.findByMercadoId(m.getId());
            long totalCurtidas = 0;
            for (Oferta o : ofertas) {
                totalCurtidas += curtidaRepository.countByIdOferta(o.getId());
            }
            long totalFavoritos = favoritoRepository.countByIdMercado(m.getId());
            
            rankingTemp.add(new MercadoRankingDTO(
                m.getId(),
                m.getNome(),
                m.getImagemLogo(),
                totalCurtidas,
                totalFavoritos,
                0
            ));
        }

        // Ordenar por totalCurtidas desc, depois totalFavoritos desc, depois nome
        rankingTemp.sort((a, b) -> {
            int comp = Long.compare(b.totalCurtidas(), a.totalCurtidas());
            if (comp != 0) return comp;
            int compFav = Long.compare(b.totalFavoritos(), a.totalFavoritos());
            if (compFav != 0) return compFav;
            return a.nome().compareToIgnoreCase(b.nome());
        });

        // Preencher a posição
        List<MercadoRankingDTO> rankingFinal = new ArrayList<>();
        for (int i = 0; i < rankingTemp.size(); i++) {
            MercadoRankingDTO r = rankingTemp.get(i);
            rankingFinal.add(new MercadoRankingDTO(
                r.id(),
                r.nome(),
                r.imagemLogo(),
                r.totalCurtidas(),
                r.totalFavoritos(),
                i + 1
            ));
        }

        return rankingFinal;
    }
}
