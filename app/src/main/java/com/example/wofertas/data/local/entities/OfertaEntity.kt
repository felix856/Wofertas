package com.example.wofertas.data.local.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "ofertas_cache")
data class OfertaEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val status: String?,
    val data: String?,
    val imagemOferta: String?,
    val mercadoId: String,
    val mercadoNome: String,
    val mercadoCnpj: String?,
    val mercadoEndereco: String?,
    val mercadoImagemLogo: String?,
    val mercadoEmail: String,
    val mercadoLatitude: Double?,
    val mercadoLongitude: Double?,
    val cachedAt: Long = System.currentTimeMillis()
) : Parcelable