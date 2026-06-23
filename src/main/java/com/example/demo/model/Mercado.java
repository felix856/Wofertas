package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Document(collection = "mercado")
public class Mercado {

    @Id
    private String id;           // ObjectId do MongoDB (era Long no MySQL)

    private String nome;
    private String cnpj;
    private String endereco;
    private String telefone;     // ← adicionado conforme diagrama UML

    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    private String senha;

    private String imagemLogo;

    // Coordenadas geográficas — populadas automaticamente via Nominatim
    private Double latitude;
    private Double longitude;

    private String resetToken;
    private LocalDateTime resetTokenExpiration;

    private String privacyPolicyVersion;
    private String termsVersion;
    private LocalDateTime privacyAcceptedAt;
    private LocalDateTime termsAcceptedAt;
    private Boolean marketingConsent;
    private Boolean analyticsConsent;
    private LocalDateTime deletionRequestedAt;

    public Mercado() {}

    public Mercado(String nome, String cnpj, String endereco, String telefone,
                   String email, String senha, String imagemLogo) {
        this.nome      = nome;
        this.cnpj      = cnpj;
        this.endereco  = endereco;
        this.telefone  = telefone;
        this.email     = email;
        this.senha     = senha;
        this.imagemLogo = imagemLogo;
    }

    // ── Getters e Setters ─────────────────────────────────────────────────────

    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }

    public String getNome()                        { return nome; }
    public void setNome(String nome)               { this.nome = nome; }

    public String getCnpj()                        { return cnpj; }
    public void setCnpj(String cnpj)               { this.cnpj = cnpj; }

    public String getEndereco()                    { return endereco; }
    public void setEndereco(String endereco)       { this.endereco = endereco; }

    public String getTelefone()                    { return telefone; }
    public void setTelefone(String telefone)       { this.telefone = telefone; }

    public String getEmail()                       { return email; }
    public void setEmail(String email)             { this.email = email; }

    public String getSenha()                       { return senha; }
    public void setSenha(String senha)             { this.senha = senha; }

    public String getImagemLogo()                  { return imagemLogo; }
    public void setImagemLogo(String imagemLogo)   { this.imagemLogo = imagemLogo; }

    public Double getLatitude()                    { return latitude; }
    public void setLatitude(Double latitude)       { this.latitude = latitude; }

    public Double getLongitude()                   { return longitude; }
    public void setLongitude(Double longitude)     { this.longitude = longitude; }

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
