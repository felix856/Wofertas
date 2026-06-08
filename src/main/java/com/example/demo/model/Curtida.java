package com.example.demo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "curtida")
public class Curtida {

    @Id
    private String id;

    private String idOferta;      // referência à Oferta
    private String idUsuario;     // referência ao Usuário
    private LocalDateTime dataCurtida;

    public Curtida() {}

    public Curtida(String idOferta, String idUsuario) {
        this.idOferta = idOferta;
        this.idUsuario = idUsuario;
        this.dataCurtida = LocalDateTime.now();
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdOferta() { return idOferta; }
    public void setIdOferta(String idOferta) { this.idOferta = idOferta; }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public LocalDateTime getDataCurtida() { return dataCurtida; }
    public void setDataCurtida(LocalDateTime dataCurtida) { this.dataCurtida = dataCurtida; }
}
