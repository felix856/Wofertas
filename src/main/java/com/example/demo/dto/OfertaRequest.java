package com.example.demo.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body do POST /ofertas e PUT /ofertas/{id}
 * (usado pelo app ao publicar/editar via JSON body – não multipart)
 */
public class OfertaRequest {
    @NotBlank(message = "Nome da oferta é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    private String nome;

    @NotBlank(message = "Status é obrigatório")
    private String status;

    @NotNull(message = "Data é obrigatória")
    private LocalDate data;

    @Size(max = 10485760, message = "Imagem muito grande (máx 10MB)")
    private String imagemOferta;  // URL ou base64 da imagem

    public OfertaRequest() {}

    public String    getNome()                    { return nome; }
    public void      setNome(String nome)         { this.nome = nome; }
    public String    getStatus()                  { return status; }
    public void      setStatus(String s)          { this.status = s; }
    public LocalDate getData()                    { return data; }
    public void      setData(LocalDate d)         { this.data = d; }
    public String    getImagemOferta()            { return imagemOferta; }
    public void      setImagemOferta(String img)  { this.imagemOferta = img; }
}