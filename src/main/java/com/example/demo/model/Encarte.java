package com.example.demo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "encarte")
public class Encarte {

    @Id
    private String id;

    private String mercadoId;
    private String titulo;
    private String urlPdf;
    private String nomeArquivoOriginal;
    private LocalDateTime dataCriacao;
    private boolean boosted;
    private int boostLevel;
    private String boostSource;
    private LocalDateTime boostExpiresAt;

    public Encarte() {}

    public Encarte(String mercadoId, String titulo, String urlPdf, String nomeArquivoOriginal) {
        this.mercadoId = mercadoId;
        this.titulo = titulo;
        this.urlPdf = urlPdf;
        this.nomeArquivoOriginal = nomeArquivoOriginal;
        this.dataCriacao = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMercadoId() { return mercadoId; }
    public void setMercadoId(String mercadoId) { this.mercadoId = mercadoId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getUrlPdf() { return urlPdf; }
    public void setUrlPdf(String urlPdf) { this.urlPdf = urlPdf; }

    public String getNomeArquivoOriginal() { return nomeArquivoOriginal; }
    public void setNomeArquivoOriginal(String nomeArquivoOriginal) { this.nomeArquivoOriginal = nomeArquivoOriginal; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public boolean isBoosted() { return boosted; }
    public void setBoosted(boolean boosted) { this.boosted = boosted; }

    public int getBoostLevel() { return boostLevel; }
    public void setBoostLevel(int boostLevel) { this.boostLevel = boostLevel; }

    public String getBoostSource() { return boostSource; }
    public void setBoostSource(String boostSource) { this.boostSource = boostSource; }

    public LocalDateTime getBoostExpiresAt() { return boostExpiresAt; }
    public void setBoostExpiresAt(LocalDateTime boostExpiresAt) { this.boostExpiresAt = boostExpiresAt; }
}
