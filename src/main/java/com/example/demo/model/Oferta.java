package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "oferta")
public class Oferta {

    @Id
    private String id;           // ObjectId do MongoDB (era Long no MySQL)

    private String nome;
    private String status;       // "ATIVO" ou "INATIVO"

    private LocalDate dataInicio;  // ← separado conforme diagrama UML
    private LocalDate dataFim;     // ← separado conforme diagrama UML

    private String imagemOferta;

    // Referência ao mercado dono desta oferta (armazenamos só o ID — padrão MongoDB)
    private String mercadoId;
    private String mercadoNome;    // desnormalizado para evitar joins
    private String mercadoLogo;    // desnormalizado para exibir no feed sem chamada extra

    private boolean boosted;
    private int boostLevel;
    private String boostSource;     // plan, manual, paid
    private LocalDateTime boostExpiresAt;

    public Oferta() {}

    // ── Getters e Setters ─────────────────────────────────────────────────────

    public String getId()                              { return id; }
    public void setId(String id)                       { this.id = id; }

    public String getNome()                            { return nome; }
    public void setNome(String nome)                   { this.nome = nome; }

    public String getStatus()                          { return status; }
    public void setStatus(String status)               { this.status = status; }

    public LocalDate getDataInicio()                   { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio)    { this.dataInicio = dataInicio; }

    public LocalDate getDataFim()                      { return dataFim; }
    public void setDataFim(LocalDate dataFim)          { this.dataFim = dataFim; }

    public String getImagemOferta()                    { return imagemOferta; }
    public void setImagemOferta(String imagemOferta)   { this.imagemOferta = imagemOferta; }

    public String getMercadoId()                       { return mercadoId; }
    public void setMercadoId(String mercadoId)         { this.mercadoId = mercadoId; }

    public String getMercadoNome()                     { return mercadoNome; }
    public void setMercadoNome(String mercadoNome)     { this.mercadoNome = mercadoNome; }

    public String getMercadoLogo()                     { return mercadoLogo; }
    public void setMercadoLogo(String mercadoLogo)     { this.mercadoLogo = mercadoLogo; }

    public boolean isBoosted()                         { return boosted; }
    public void setBoosted(boolean boosted)            { this.boosted = boosted; }

    public int getBoostLevel()                         { return boostLevel; }
    public void setBoostLevel(int boostLevel)          { this.boostLevel = boostLevel; }

    public String getBoostSource()                     { return boostSource; }
    public void setBoostSource(String boostSource)     { this.boostSource = boostSource; }

    public LocalDateTime getBoostExpiresAt()           { return boostExpiresAt; }
    public void setBoostExpiresAt(LocalDateTime boostExpiresAt) { this.boostExpiresAt = boostExpiresAt; }
}
