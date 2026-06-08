package com.example.demo.dto;

public class OfertaAnalyticsDTO {

    private String id;
    private String nome;
    private String status;
    private String imagemOferta;
    private long curtidas;
    private long visualizacoes;
    private long itensCarrinho;
    private double engajamento; // (curtidas + visualizações) / alguma métrica

    public OfertaAnalyticsDTO() {}

    public OfertaAnalyticsDTO(
        String id,
        String nome,
        String status,
        String imagemOferta,
        long curtidas,
        long visualizacoes,
        long itensCarrinho,
        double engajamento
    ) {
        this.id = id;
        this.nome = nome;
        this.status = status;
        this.imagemOferta = imagemOferta;
        this.curtidas = curtidas;
        this.visualizacoes = visualizacoes;
        this.itensCarrinho = itensCarrinho;
        this.engajamento = engajamento;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImagemOferta() { return imagemOferta; }
    public void setImagemOferta(String imagemOferta) { this.imagemOferta = imagemOferta; }

    public long getCurtidas() { return curtidas; }
    public void setCurtidas(long curtidas) { this.curtidas = curtidas; }

    public long getVisualizacoes() { return visualizacoes; }
    public void setVisualizacoes(long visualizacoes) { this.visualizacoes = visualizacoes; }

    public long getItensCarrinho() { return itensCarrinho; }
    public void setItensCarrinho(long itensCarrinho) { this.itensCarrinho = itensCarrinho; }

    public double getEngajamento() { return engajamento; }
    public void setEngajamento(double engajamento) { this.engajamento = engajamento; }
}
