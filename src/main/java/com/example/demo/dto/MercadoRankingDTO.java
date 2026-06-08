package com.example.demo.dto;

public record MercadoRankingDTO(
    String id,
    String nome,
    String imagemLogo,
    long totalCurtidas,
    long totalFavoritos,
    int posicao
) {}
