package com.example.demo.dto;

public class EncarteDTO {

    private String id;
    private String mercadoId;
    private String titulo;
    private String urlPdf;
    private String dataCriacao;

    public EncarteDTO() {}

    public EncarteDTO(String id, String mercadoId, String titulo, String urlPdf, String dataCriacao) {
        this.id = id;
        this.mercadoId = mercadoId;
        this.titulo = titulo;
        this.urlPdf = urlPdf;
        this.dataCriacao = dataCriacao;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMercadoId() { return mercadoId; }
    public void setMercadoId(String mercadoId) { this.mercadoId = mercadoId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getUrlPdf() { return urlPdf; }
    public void setUrlPdf(String urlPdf) { this.urlPdf = urlPdf; }

    public String getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(String dataCriacao) { this.dataCriacao = dataCriacao; }
}
