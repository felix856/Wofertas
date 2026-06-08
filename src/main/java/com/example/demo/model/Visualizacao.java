package com.example.demo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "visualizacao")
public class Visualizacao {

    @Id
    private String id;

    private String idOferta;        // referência à Oferta
    private String idUsuario;       // referência ao Usuário (null se anônimo)
    private LocalDateTime dataVisualizacao;
    private String origem;          // "DASHBOARD", "FEED", "BUSCA"

    public Visualizacao() {}

    public Visualizacao(String idOferta, String idUsuario, String origem) {
        this.idOferta = idOferta;
        this.idUsuario = idUsuario;
        this.origem = origem;
        this.dataVisualizacao = LocalDateTime.now();
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdOferta() { return idOferta; }
    public void setIdOferta(String idOferta) { this.idOferta = idOferta; }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public LocalDateTime getDataVisualizacao() { return dataVisualizacao; }
    public void setDataVisualizacao(LocalDateTime dataVisualizacao) { this.dataVisualizacao = dataVisualizacao; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
}
