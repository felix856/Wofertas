package com.example.wofertas;

import java.io.Serializable;

// Esta é a estrutura final para uma oferta.
// Ela implementa Serializable para poder ser passada entre Activities (para o Mapa).
public class Oferta implements Serializable {

    // --- Campos Salvos no Firebase ---
    private String ofertaId;    // ID do documento da oferta
    private String nome;         // Nome do Supermercado (buscado do perfil do usuário)
    private String descricao;    // Título da Oferta (ex: "Fim de Semana", "Quarta do Hortifruti")
    private String urlPdf;       // URL do PDF no Firebase Storage
    private String lojaId;       // ID do usuário (supermercado) que publicou a oferta
    private long timestamp;      // Data e hora da publicação, para ordenação
    private Double latitude;     // Latitude do supermercado
    private Double longitude;    // Longitude do supermercado
    private String thumbUrl;     // Opcional: URL de uma imagem de miniatura

    // --- Campo Local (não salvo no Firebase) ---
    // Este campo será calculado em tempo de execução no app do cliente.
    private transient double distancia;

    // Construtor vazio é OBRIGATÓRIO para o Firebase/Firestore funcionar
    public Oferta() {}

    // --- Getters e Setters para todos os campos ---

    public String getOfertaId() { return ofertaId; }
    public void setOfertaId(String ofertaId) { this.ofertaId = ofertaId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getUrlPdf() { return urlPdf; }
    public void setUrlPdf(String urlPdf) { this.urlPdf = urlPdf; }

    public String getLojaId() { return lojaId; }
    public void setLojaId(String lojaId) { this.lojaId = lojaId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getThumbUrl() { return thumbUrl; }
    public void setThumbUrl(String thumbUrl) { this.thumbUrl = thumbUrl; }

    public double getDistancia() { return distancia; }
    public void setDistancia(double distancia) { this.distancia = distancia; }
}
