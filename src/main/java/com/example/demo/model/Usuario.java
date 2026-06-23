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

    private String privacyPolicyVersion;
    private String termsVersion;
    private LocalDateTime privacyAcceptedAt;
    private LocalDateTime termsAcceptedAt;
    private Boolean marketingConsent;
    private Boolean analyticsConsent;
    private LocalDateTime deletionRequestedAt;

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

    public String getPrivacyPolicyVersion() { return privacyPolicyVersion; }
    public void setPrivacyPolicyVersion(String privacyPolicyVersion) { this.privacyPolicyVersion = privacyPolicyVersion; }

    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }

    public LocalDateTime getPrivacyAcceptedAt() { return privacyAcceptedAt; }
    public void setPrivacyAcceptedAt(LocalDateTime privacyAcceptedAt) { this.privacyAcceptedAt = privacyAcceptedAt; }

    public LocalDateTime getTermsAcceptedAt() { return termsAcceptedAt; }
    public void setTermsAcceptedAt(LocalDateTime termsAcceptedAt) { this.termsAcceptedAt = termsAcceptedAt; }

    public Boolean getMarketingConsent() { return marketingConsent; }
    public void setMarketingConsent(Boolean marketingConsent) { this.marketingConsent = marketingConsent; }

    public Boolean getAnalyticsConsent() { return analyticsConsent; }
    public void setAnalyticsConsent(Boolean analyticsConsent) { this.analyticsConsent = analyticsConsent; }

    public LocalDateTime getDeletionRequestedAt() { return deletionRequestedAt; }
    public void setDeletionRequestedAt(LocalDateTime deletionRequestedAt) { this.deletionRequestedAt = deletionRequestedAt; }
}
