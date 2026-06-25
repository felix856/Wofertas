package com.example.demo.dto;

public record ChatbotRequest(
        String mensagem,
        String pagina,
        String contextoTela
) {}
