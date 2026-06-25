package com.example.demo.dto;

import java.time.Instant;

public record ChatbotResponse(
        String resposta,
        String tipoUsuario,
        String modo,
        String modelo,
        boolean contextoAnaliticoUsado,
        Instant respondidoEm
) {}
