package com.example.wofertas.data.local.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────
// MERCADOS
// ─────────────────────────────────────────────────────────

@Entity(tableName = "mercados_cache")
data class MercadoEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val cnpj: String?,
    val endereco: String?,
    val telefone: String?,
    val email: String,
    val imagemLogo: String?,
    val latitude: Double?,
    val longitude: Double?,
    val cachedAt: Long = System.currentTimeMillis()
)


// ─────────────────────────────────────────────────────────
// FAVORITOS
// ─────────────────────────────────────────────────────────

@Entity(
    tableName = "favoritos_cache",
    primaryKeys = ["usuarioId", "mercadoId"]
)
data class FavoritoEntity(
    val id: String,
    val usuarioId: String,
    val mercadoId: String,
    val mercadoNome: String = "",
    val mercadoImagemLogo: String? = null,
    val mercadoEndereco: String? = null,
    val pendingSync: Boolean = false
)


// ─────────────────────────────────────────────────────────
// ENCARTES
// ─────────────────────────────────────────────────────────

@Entity(tableName = "encartes_cache")
data class EncarteEntity(
    @PrimaryKey val id: String,
    val mercadoId: String,
    val titulo: String,
    val urlPdf: String,
    val dataCriacao: String?,
    val cachedAt: Long = System.currentTimeMillis()
)


// ─────────────────────────────────────────────────────────
// LISTA DE COMPRAS
// ─────────────────────────────────────────────────────────

@Entity(tableName = "lista_compras")
data class ProdutoListaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,

    // preço máximo que o usuário aceita pagar
    val precoMaximo: Double? = null
)


// ─────────────────────────────────────────────────────────
// PRODUTOS ENCONTRADOS (OCR)
// ─────────────────────────────────────────────────────────

@Entity(tableName = "produtos_encontrados")
data class ProdutoEncontradoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nomeProduto: String,      // nome buscado na lista
    val nomeEncontrado: String,   // texto exato do OCR
    val preco: Double?,
    val nomeMercado: String,
    val ofertaId: String,

    val encontradoEm: Long = System.currentTimeMillis()
)