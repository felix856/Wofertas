package com.example.demo.dto;

import jakarta.validation.constraints.*;

/**
 * DTO para atualização flexível de Mercado.
 * Campos nulos ou vazios serão ignorados no Service para manter os dados atuais.
 */
public class MercadoUpdateDTO {

    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    private String nome;

    @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos")
    private String cnpj;

    @Size(min = 5, max = 500, message = "Endereço deve ter entre 5 e 500 caracteres")
    private String endereco;

    @Email(message = "Email deve ser válido")
    private String email;

    // Senha opcional: só valida se o usuário digitar algo
    @Size(min = 6, max = 128, message = "A nova senha deve ter entre 6 e 128 caracteres")
    private String senha;

    private String imagemLogo;

    @DecimalMin(value = "-90.0", message = "Latitude inválida")
    @DecimalMax(value = "90.0", message = "Latitude inválida")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude inválida")
    @DecimalMax(value = "180.0", message = "Longitude inválida")
    private Double longitude;

    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos")
    private String telefone;

    public MercadoUpdateDTO() {}

    // --- Getters e Setters ---

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getImagemLogo() { return imagemLogo; }
    public void setImagemLogo(String imagemLogo) { this.imagemLogo = imagemLogo; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
