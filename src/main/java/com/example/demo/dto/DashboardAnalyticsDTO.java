package com.example.demo.dto;

import java.util.List;
import java.util.Map;

public record DashboardAnalyticsDTO(
    long totalVisualizacoes,
    long totalCurtidas,
    long totalFavoritos,
    long totalItensCarrinho,
    long totalEncartes,
    double taxaConversaoVisualizacoesCurtidas,
    double taxaConversaoVisualizacoesCarrinho,
    List<OfertaAnalyticsDTO> encartesRanking,
    List<OfertaAnalyticsDTO> encartesComMaiorCurtidas,
    List<OfertaAnalyticsDTO> encartesComMaiorCarrinho,
    Map<String, Long> produtosComMaiorCurtidas,
    Map<String, Long> produtosComMaiorCarrinho,
    Map<String, Long> visualizacoesPorOrigem,
    InsightEstrategicoDTO insight
) {}
