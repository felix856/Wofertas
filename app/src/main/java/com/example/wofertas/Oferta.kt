// app/src/main/java/com/example/wofertas/Oferta.kt
package com.example.wofertas

import java.io.Serializable

/**
 * Modelo de Oferta alinhado com a API Spring Boot + MongoDB.
 *
 * MUDANÇA: mercadoId era Long (banco relacional); agora é String (ObjectId MongoDB).
 *
 * Mapeamento API → App:
 *   OfertaDto.id                → ofertaId       (String ObjectId)
 *   OfertaDto.nome              → nome
 *   OfertaDto.status            → status
 *   OfertaDto.data              → dataValidade    ("yyyy-MM-dd")
 *   OfertaDto.imagemOferta      → imagemOferta
 *   OfertaDto.mercado.id        → mercadoId       (String ObjectId)
 *   OfertaDto.mercado.nome      → nomeSupermercado
 *   OfertaDto.mercado.endereco  → enderecoSupermercado
 *   OfertaDto.mercado.imagemLogo → imagemLogo
 *   OfertaDto.mercado.latitude  → latitude
 *   OfertaDto.mercado.longitude → longitude
 */
class Oferta : Serializable {

    // ── Identificação ──────────────────────────────────────────────────────────
    var ofertaId:  String? = null   // ObjectId da oferta
    var mercadoId: String? = null   // ObjectId do mercado  ← era Long

    // ── Dados da oferta ────────────────────────────────────────────────────────
    var nome:         String? = null
    var status:       String? = null
    var dataValidade: String? = null
    var imagemOferta: String? = null

    // ── Dados do mercado vinculado ─────────────────────────────────────────────
    var nomeSupermercado:     String? = null
    var enderecoSupermercado: String? = null
    var imagemLogo:           String? = null
    var latitude:             Double? = null
    var longitude:            Double? = null

    // ── Calculados localmente (não vêm da API) ─────────────────────────────────
    var distancia: Double = Double.MAX_VALUE
    @Transient
    var isSaved: Boolean = false
}
