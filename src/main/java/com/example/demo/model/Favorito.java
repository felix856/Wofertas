package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "favorito")
public class Favorito {

    @Id
    private String id;           // ObjectId do MongoDB (era Long no MySQL)

    // No MongoDB não usamos @ManyToOne/@JoinColumn — guardamos os IDs diretamente
    private String idUsuario;    // referência ao Usuario
    private String idMercado;    // referência ao Mercado

    public Favorito() {}

    public Favorito(String idUsuario, String idMercado) {
        this.idUsuario = idUsuario;
        this.idMercado = idMercado;
    }

    // ── Getters e Setters ─────────────────────────────────────────────────────

    public String getId()                      { return id; }
    public void setId(String id)               { this.id = id; }

    public String getIdUsuario()               { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getIdMercado()               { return idMercado; }
    public void setIdMercado(String idMercado) { this.idMercado = idMercado; }
}