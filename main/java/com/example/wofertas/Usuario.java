package com.example.wofertas;

import java.io.Serializable;

public class Usuario implements Serializable {
    private String id;
    private String nome; // Para clientes
    private String nomeLoja; // Para supermercados
    private String email;
    private String tipo; // "cliente" ou "supermercado"
    private String telefone;
    private String cnpj;
    private String endereco; // Para geocodificação do supermercado
    private Double latitude;
    private Double longitude;

    public Usuario() {
        // Construtor vazio para o Firestore
    }

    // Adicione Getters e Setters para todos os campos
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getNomeLoja() { return nomeLoja; }
    public void setNomeLoja(String nomeLoja) { this.nomeLoja = nomeLoja; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
