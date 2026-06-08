package com.example.demo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "item_carrinho")
public class ItemCarrinho {

    @Id
    private String id;

    private String idUsuario;      // referência ao Usuário
    private String idOferta;       // referência à Oferta
    private String nomeOferta;     // desnormalizado para analytics
    private String mercadoId;      // desnormalizado para saber qual mercado é
    private int quantidade;
    private LocalDateTime dataAdicao;

    public ItemCarrinho() {}

    public ItemCarrinho(String idUsuario, String idOferta, String nomeOferta, String mercadoId, int quantidade) {
        this.idUsuario = idUsuario;
        this.idOferta = idOferta;
        this.nomeOferta = nomeOferta;
        this.mercadoId = mercadoId;
        this.quantidade = quantidade;
        this.dataAdicao = LocalDateTime.now();
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
