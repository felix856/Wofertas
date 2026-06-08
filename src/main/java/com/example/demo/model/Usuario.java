package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Document(collection = "usuario")
public class Usuario {

    @Id
    private String id;           // ObjectId do MongoDB (era Long no MySQL)

    private String nome;

    @Indexed(unique = true)      // garante email único no MongoDB
    private String email;

    @JsonIgnore
    private String senha;

    private String imagemPerfil; // URL ou Base64 da imagem de perfil do cliente

    private String resetToken;
    private LocalDateTime resetTokenExpiration;

    public Usuario() {}

    public Usuario(String nome, String email, String senha) {
        this.nome  = nome;
        this.email = email;
        this.senha = senha;
    }

    // ── Getters e Setters ─────────────────────────────────────────────────────

    public String getId()                 { return id; }
    public void setId(String id)          { this.id = id; }

    public String getNome()               { return nome; }
    public void setNome(String nome)      { this.nome = nome; }

    public String getEmail()              { return email; }
    public void setEmail(String email)    { this.email = email; }

    public String getSenha()              { return senha; }
    public void setSenha(String senha)    { this.senha = senha; }

    public String getImagemPerfil() { return imagemPerfil; }
    public void setImagemPerfil(String imagemPerfil) { this.imagemPerfil = imagemPerfil; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiration() { return resetTokenExpiration; }
    public void setResetTokenExpiration(LocalDateTime resetTokenExpiration) { this.resetTokenExpiration = resetTokenExpiration; }
}