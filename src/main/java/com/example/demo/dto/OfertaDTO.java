package com.example.demo.dto;

/**
 * DTO de resposta para GET /ofertas, GET /ofertas/historico, GET /ofertas/favoritas.
 *
 * Embute os dados do Mercado dentro da oferta para que o app Android
 * não precise fazer uma segunda chamada para buscar o mercado.
 *
 * Estrutura retornada ao app:
 * {
 *   "id": "665a...",
 *   "nome": "Encarte Semanal",
 *   "status": "ATIVO",
 *   "data": "2025-08-01",
 *   "imagemOferta": "https://...",
 *   "mercado": {
 *     "id": "664b...",
 *     "nome": "Supermercado ABC",
 *     "cnpj": "12345678000195",
 *     "endereco": "Rua X, 100",
 *     "imagemLogo": "https://...",
 *     "email": "abc@email.com",
 *     "latitude": -27.59,
 *     "longitude": -48.54
 *   }
 * }
 */
public class OfertaDTO {

    private String id;
    private String nome;
    private String status;
    private String data;          // ISO: "yyyy-MM-dd"
    private String imagemOferta;
    private MercadoResumoDTO mercado;
    

    public OfertaDTO() {}

    // ── Getters e Setters ──────────────────────────────────────────────────────

    public String getId()                         { return id; }
    public void   setId(String id)                { this.id = id; }

    public String getNome()                       { return nome; }
    public void   setNome(String nome)            { this.nome = nome; }

    public String getStatus()                     { return status; }
    public void   setStatus(String status)        { this.status = status; }

    public String getData()                       { return data; }
    public void   setData(String data)            { this.data = data; }

    public String getImagemOferta()               { return imagemOferta; }
    public void   setImagemOferta(String img)     { this.imagemOferta = img; }

    public MercadoResumoDTO getMercado()          { return mercado; }
    public void   setMercado(MercadoResumoDTO m)  { this.mercado = m; }

    // ── Inner DTO ──────────────────────────────────────────────────────────────

    public static class MercadoResumoDTO {
        private String id;
        private String nome;
        private String cnpj;
        private String endereco;
        private String imagemLogo;
        private String email;
        private Double latitude;
        private Double longitude;

        public MercadoResumoDTO() {}

        public String getId()                         { return id; }
        public void   setId(String id)                { this.id = id; }
        public String getNome()                       { return nome; }
        public void   setNome(String nome)            { this.nome = nome; }
        public String getCnpj()                       { return cnpj; }
        public void   setCnpj(String cnpj)            { this.cnpj = cnpj; }
        public String getEndereco()                   { return endereco; }
        public void   setEndereco(String endereco)    { this.endereco = endereco; }
        public String getImagemLogo()                 { return imagemLogo; }
        public void   setImagemLogo(String img)       { this.imagemLogo = img; }
        public String getEmail()                      { return email; }
        public void   setEmail(String email)          { this.email = email; }
        public Double getLatitude()                   { return latitude; }
        public void   setLatitude(Double lat)         { this.latitude = lat; }
        public Double getLongitude()                  { return longitude; }
        public void   setLongitude(Double lon)        { this.longitude = lon; }
    }
}