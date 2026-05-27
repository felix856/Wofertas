package com.example.wofertas.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wofertas.CarrinhoActivity
import com.example.wofertas.ListaOfertas
import com.example.wofertas.R

object NotificationHelper {

    const val CH_PROXIMIDADE = "wofertas_proximidade"
    const val CH_NOVAS_OFERTAS = "wofertas_novas_ofertas"
    const val CH_LISTA_COMPRAS = "wofertas_lista_compras"

    private const val PREFS = "notif_prefs"
    private const val KEY_SEEN_IDS = "seen_offer_ids"

    fun criarCanais(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val canais = listOf(
            Triple(CH_PROXIMIDADE, "Ofertas Próximas", NotificationManager.IMPORTANCE_HIGH),
            Triple(CH_NOVAS_OFERTAS, "Novas Ofertas", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CH_LISTA_COMPRAS, "Lista de Compras", NotificationManager.IMPORTANCE_HIGH)
        )

        canais.forEach { (id, nome, imp) ->
            if (mgr.getNotificationChannel(id) == null) {
                mgr.createNotificationChannel(NotificationChannel(id, nome, imp))
            }
        }
    }

    fun notificarProximidade(context: Context, titulo: String, corpo: String, tag: String) {
        disparar(context, CH_PROXIMIDADE, tag.hashCode(), titulo, corpo, ListaOfertas::class.java)
    }

    fun notificarNovasOfertas(context: Context, idsNovos: List<String>, nomesNovos: List<String>): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val vistos = prefs.getStringSet(KEY_SEEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val novos = idsNovos.filterNot { vistos.contains(it) }
        if (novos.isEmpty()) return false
        
        vistos.addAll(novos)
        prefs.edit().putStringSet(KEY_SEEN_IDS, vistos).apply()

        val qtd = novos.size
        val titulo = if (qtd == 1) "Nova oferta disponível!" else "$qtd novas ofertas!"
        val corpo = nomesNovos.take(3).joinToString(" | ").let {
            if (nomesNovos.size > 3) "$it e mais ${nomesNovos.size - 3}" else it
        }
        disparar(context, CH_NOVAS_OFERTAS, 9001, titulo, corpo, ListaOfertas::class.java)
        return true
    }

    fun notificarProdutoEncontrado(
        context: Context,
        produto: String,
        mercado: String,
        preco: String,
        notifId: Int,
        recorrente: Boolean = false
    ) {
        val titulo = if (recorrente) "🔄 Item Recorrente: $produto" else "Sugestão: $produto"
        val corpo = if (recorrente) {
            "Detectamos sua preferência! Está em promoção no $mercado$preco."
        } else {
            "Vimos que você quer $produto. Tem no $mercado$preco!"
        }
        
        disparar(
            context,
            CH_LISTA_COMPRAS,
            notifId,
            titulo = titulo,
            corpo = corpo,
            destino = CarrinhoActivity::class.java
        )
    }

    private fun <T> disparar(
        context: Context,
        channelId: String,
        notifId: Int,
        titulo: String,
        corpo: String,
        destino: Class<T>
    ) {
        val pi = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, destino).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setPriority(if (channelId == CH_NOVAS_OFERTAS) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(notifId, notif)
    }
}
