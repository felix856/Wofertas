package com.example.demo.dto;

public class InsightEstrategicoDTO {

    private String encarteMelhorPerformance;    // Nome do encarte com melhor engagement
    private double engajamentoMedio;             // Média de engajamento de todos os encartes
    private String recomendacao;                 // Recomendação estratégica baseada nos dados
    private long clientesAtivos;                 // Número de clientes únicos que interagiram (sem dados pessoais)
    private String tendencia;             // Tendência identificada nos dados

    public InsightEstrategicoDTO() {}

    public InsightEstrategicoDTO(
        String encarteMelhorPerformance,
        double engajamentoMedio,
        String recomendacao,
        long clientesAtivos,
        String tendencia
    ) {
        this.encarteMelhorPerformance = encarteMelhorPerformance;
        this.engajamentoMedio = engajamentoMedio;
        this.recomendacao = recomendacao;
        this.clientesAtivos = clientesAtivos;
        this.tendencia = tendencia;
    }

    // Getters e Setters
    public String getEncarteMelhorPerformance() { return encarteMelhorPerformance; }
    public void setEncarteMelhorPerformance(String encarteMelhorPerformance) { this.encarteMelhorPerformance = encarteMelhorPerformance; }

    public double getEngajamentoMedio() { return engajamentoMedio; }
    public void setEngajamentoMedio(double engajamentoMedio) { this.engajamentoMedio = engajamentoMedio; }

    public String getRecomendacao() { return recomendacao; }
    public void setRecomendacao(String recomendacao) { this.recomendacao = recomendacao; }

    public long getClientesAtivos() { return clientesAtivos; }
    public void setClientesAtivos(long clientesAtivos) { this.clientesAtivos = clientesAtivos; }

    public String getTendencia() { return tendencia; }
    public void setTendencia(String tendencia) { this.tendencia = tendencia; }
}
