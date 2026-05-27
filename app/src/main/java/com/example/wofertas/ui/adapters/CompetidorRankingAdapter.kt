package com.example.wofertas.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wofertas.R
import com.example.wofertas.network.MercadoRankingDto
import com.example.wofertas.utils.loadImage
import com.google.android.material.card.MaterialCardView

class CompetidorRankingAdapter(
    private val currentMercadoId: String?
) : RecyclerView.Adapter<CompetidorRankingAdapter.ViewHolder>() {

    private val itens = mutableListOf<MercadoRankingDto>()

    fun submitList(novosItens: List<MercadoRankingDto>) {
        itens.clear()
        itens.addAll(novosItens)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_competidor_ranking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(itens[position], currentMercadoId)
    }

    override fun getItemCount(): Int = itens.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardRankingItem)
        private val position: TextView = itemView.findViewById(R.id.tvRankingPosition)
        private val logo: ImageView = itemView.findViewById(R.id.ivMercadoLogo)
        private val nome: TextView = itemView.findViewById(R.id.tvMercadoNome)
        private val badgeVoce: TextView = itemView.findViewById(R.id.tvVoceBadge)
        private val detalhes: TextView = itemView.findViewById(R.id.tvRankingDetails)
        private val score: TextView = itemView.findViewById(R.id.tvRankingScore)

        fun bind(item: MercadoRankingDto, currentMercadoId: String?) {
            val isCurrent = currentMercadoId != null && item.id == currentMercadoId
            val pontos = item.totalCurtidas + item.totalFavoritos

            position.text = "#${item.posicao}"
            nome.text = item.nome
            detalhes.text = "${item.totalCurtidas} curtidas | ${item.totalFavoritos} salvos"
            score.text = "$pontos pts"
            badgeVoce.visibility = if (isCurrent) View.VISIBLE else View.GONE
            logo.loadImage(item.imagemLogo, R.drawable.logo_supermercado_placeholder)

            if (isCurrent) {
                card.setCardBackgroundColor(Color.rgb(255, 248, 231))
                card.strokeColor = itemView.context.getColor(R.color.wofertas_yellow)
            } else {
                card.setCardBackgroundColor(itemView.context.getColor(R.color.wofertas_surface))
                card.strokeColor = itemView.context.getColor(R.color.border_light)
            }
        }
    }
}
