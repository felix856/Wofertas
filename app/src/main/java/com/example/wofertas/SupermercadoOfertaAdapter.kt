package com.example.wofertas

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wofertas.utils.loadImage
import com.google.android.material.chip.Chip

class SupermercadoOfertaAdapter(
    private var listaOfertas: MutableList<Oferta>,
    private val context: Context,
    private val listener: OnItemActionListener
) : RecyclerView.Adapter<SupermercadoOfertaAdapter.OfertaSupermercadoViewHolder>() {

    interface OnItemActionListener {
        fun onPdfClick(oferta: Oferta)
        fun onEditClick(oferta: Oferta)
        fun onDeleteClick(oferta: Oferta)
        fun onToggleStatusClick(oferta: Oferta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaSupermercadoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_oferta_supermercado, parent, false)
        return OfertaSupermercadoViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfertaSupermercadoViewHolder, position: Int) {
        val oferta = listaOfertas[position]

        holder.imgOfertaSupermercado.loadImage(oferta.imagemOferta, R.drawable.logo_supermercado_placeholder)

        holder.txtTituloOfertaSupermercado.text = oferta.nome ?: "Sem título"
        holder.txtDescricaoOfertaSupermercado.text = oferta.nomeSupermercado ?: ""
        holder.txtDataEnvio.text = "Validade: ${oferta.dataValidade ?: "-"}"

        // Lógica de Status usando suas cores do colors.xml
        val status = oferta.status?.uppercase() ?: "ATIVO"
        holder.chipStatus.text = status

        if (status == "SUSPENSO" || status == "INATIVO") {
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_suspenso)
            holder.imgToggleStatus.setImageResource(android.R.drawable.ic_media_play)
        } else {
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_ativo)
            holder.imgToggleStatus.setImageResource(android.R.drawable.ic_media_pause)
        }

        // Cliques mapeados nos botões do XML
        holder.imgViewPdf.setOnClickListener      { listener.onPdfClick(oferta) }
        holder.imgEdit.setOnClickListener         { listener.onEditClick(oferta) }
        holder.imgDelete.setOnClickListener       { listener.onDeleteClick(oferta) }
        holder.imgToggleStatus.setOnClickListener { listener.onToggleStatusClick(oferta) }
    }

    override fun getItemCount(): Int = listaOfertas.size

    fun setOfertas(novasOfertas: List<Oferta>) {
        val diffResult = DiffUtil.calculateDiff(OfertasDiffCallback(listaOfertas, novasOfertas))
        listaOfertas.clear()
        listaOfertas.addAll(novasOfertas)
        diffResult.dispatchUpdatesTo(this)
    }

    class OfertaSupermercadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgOfertaSupermercado: ImageView = itemView.findViewById(R.id.imgOfertaSupermercado)
        val txtTituloOfertaSupermercado: TextView = itemView.findViewById(R.id.txtTituloOfertaSupermercado)
        val txtDescricaoOfertaSupermercado: TextView = itemView.findViewById(R.id.txtDescricaoOfertaSupermercado)
        val txtDataEnvio: TextView = itemView.findViewById(R.id.txtDataEnvio)
        val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)

        val imgViewPdf: ImageView = itemView.findViewById(R.id.imgViewPdf)
        val imgEdit: ImageView = itemView.findViewById(R.id.imgEdit)
        val imgToggleStatus: ImageView = itemView.findViewById(R.id.imgToggleStatus)
        val imgDelete: ImageView = itemView.findViewById(R.id.imgDelete)
    }
}

class OfertasDiffCallback(private val old: List<Oferta>, private val new: List<Oferta>) : DiffUtil.Callback() {
    override fun getOldListSize() = old.size
    override fun getNewListSize() = new.size
    override fun areItemsTheSame(op: Int, np: Int) = old[op].ofertaId == new[np].ofertaId
    override fun areContentsTheSame(op: Int, np: Int) = old[op] == new[np]
}