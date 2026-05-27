package com.example.wofertas

import android.content.Context
import android.location.Location
import com.example.wofertas.fcm.NotificationHelper
import java.util.Locale

object ProximidadeNotificacao {

    const val RAIO_METROS = 10_000.0
    const val RAIO_ALERTA_FORTE_METROS = 1_000.0

    private val mercadosNotificados = mutableSetOf<String>()

    fun verificarProximidade(
        context: Context,
        localizacao: Location,
        ofertas: List<Oferta>
    ) {
        val mercadosComOfertas = ofertas
            .filter { it.latitude != null && it.longitude != null && it.mercadoId != null }
            .groupBy { it.mercadoId!! }

        for ((mercadoId, ofertasDoMercado) in mercadosComOfertas) {
            if (mercadosNotificados.contains(mercadoId)) continue

            val oferta = ofertasDoMercado.first()
            val distancia = calcularDistancia(localizacao, oferta.latitude!!, oferta.longitude!!)

            if (distancia <= RAIO_METROS) {
                mercadosNotificados.add(mercadoId)

                val distTexto = if (distancia < 1000f) {
                    "${distancia.toInt()} m"
                } else {
                    String.format(Locale.getDefault(), "%.1f km", distancia / 1000f)
                }

                val perto = distancia <= RAIO_ALERTA_FORTE_METROS
                NotificationHelper.notificarProximidade(
                    context = context,
                    titulo = if (perto) {
                        "Oferta perto de voce!"
                    } else {
                        "${oferta.nomeSupermercado ?: "Mercado proximo"} tem ofertas"
                    },
                    corpo = if (perto) {
                        "Voce esta a $distTexto de ${oferta.nomeSupermercado ?: "um mercado"}. Toque para conferir."
                    } else {
                        "${oferta.nomeSupermercado ?: "Mercado"} esta a $distTexto. Confira as ofertas no caminho."
                    },
                    tag = mercadoId
                )
            }
        }
    }

    fun limparHistorico() = mercadosNotificados.clear()

    private fun calcularDistancia(origem: Location, lat: Double, lon: Double): Float {
        val destino = Location("").apply {
            latitude = lat
            longitude = lon
        }
        return origem.distanceTo(destino)
    }
}
