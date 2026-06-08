package com.example.demo.dto;

import java.time.LocalDateTime;

public class ItemCarrinhoDTO {

    private String id;
    private String idUsuario;   // necessário para o mobile (ItemCarrinhoResponse)
    private String idOferta;    // necessário para o mobile (ItemCarrinhoResponse)
    private String nomeOferta;
    private String mercadoId;   // necessário para o mobile (ItemCarrinhoResponse)
    private int quantidade;
    private LocalDateTime dataAdicao;

    public ItemCarrinhoDTO() {}

    public ItemCarrinhoDTO(String id, String idUsuario, String idOferta,
                           String nomeOferta, String mercadoId, int quantidade,
                           LocalDateTime dataAdicao) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.idOferta = idOferta;
        this.nomeOferta = nomeOferta;
        this.mercadoId = mercadoId;
        this.quantidade = quantidade;
        this.dataAdicao = dataAdicao;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getIdOferta() { return idOferta; }
    public void setIdOferta(String idOferta) { this.idOferta = idOferta; }

    public String getNomeOferta() { return nomeOferta; }
    public void setNomeOferta(String nomeOferta) { this.nomeOferta = nomeOferta; }

    public String getMercadoId() { return mercadoId; }
    public void setMercadoId(String mercadoId) { this.mercadoId = mercadoId; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public LocalDateTime getDataAdicao() { return dataAdicao; }
    public void setDataAdicao(LocalDateTime dataAdicao) { this.dataAdicao = dataAdicao; }
}
