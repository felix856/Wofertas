package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class MercadoDTO {

    @NotBlank(message = "Nome do mercado é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    private String nome;

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos")
    private String cnpj;

    @NotBlank(message = "Endereço é obrigatório")
    @Size(min = 5, max = 500, message = "Endereço deve ter entre 5 e 500 caracteres")
    private String endereco;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 128, message = "Senha deve ter entre 6 e 128 caracteres")
    private String senha;

    // ⚠️ Corrigido: validação de tamanho de String (não é bytes reais, mas evita abuso)
    @Size(max = 5000000, message = "Imagem muito grande")
    private String imagemLogo;

    // Coordenadas opcionais (validadas se vierem)
    @DecimalMin(value = "-90.0", message = "Latitude inválida")
    @DecimalMax(value = "90.0", message = "Latitude inválida")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude inválida")
    @DecimalMax(value = "180.0", message = "Longitude inválida")
    private Double longitude;

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos")
    private String telefone;

    public MercadoDTO() {}

    // 🔥 Getters seguros (evita null safety warning)
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome != null ? nome.trim() : null; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj != null ? cnpj.trim() : null; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco != null ? endereco.trim() : null; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email.trim().toLowerCase() : null; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getImagemLogo() { return imagemLogo; }
    public void setImagemLogo(String imagemLogo) { this.imagemLogo = imagemLogo; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) {
        this.telefone = telefone != null ? telefone.replaceAll("\\D", "") : null;
    }
}