package com.example.wofertas

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wofertas.utils.loadImage
import java.text.DecimalFormat
import java.util.Collections
import java.util.Locale

class OfertaAdapter(
    ofertasIniciais: List<Oferta>,
    private val listener: OnOfertaClickListener?
) : RecyclerView.Adapter<OfertaAdapter.OfertaViewHolder>(), Filterable {

    private val listaVisiveis: MutableList<Oferta> = ArrayList(ofertasIniciais)
    private val listaCompleta: MutableList<Oferta> = ArrayList(ofertasIniciais)
    private val context: Context? = listener as? Context
    private var ultimaLocalizacao: Location? = null

    interface OnOfertaClickListener {
        fun onOfertaClick(oferta: Oferta)
        fun onSaveClick(oferta: Oferta, position: Int)
    }

    fun setUltimaLocalizacaoUsuario(location: Location?) {
        this.ultimaLocalizacao = location
    }
    fun updateList(novaLista: List<Oferta>) {
        // 1. Atualiza a lista que serve de base para a busca
        this.listaCompleta.clear()
        this.listaCompleta.addAll(novaLista)

        // 2. Atualiza a lista que está sendo exibida na tela agora
        this.listaVisiveis.clear()
        this.listaVisiveis.addAll(novaLista)

        // 3. Avisa o Android para redesenhar a lista
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun atualizarListaCompleta(novaLista: List<Oferta>) {
        listaCompleta.clear()
        listaCompleta.addAll(novaLista)
        // CORREÇÃO: Garante que a lista visível também receba os dados antes do filtro
        listaVisiveis.clear()
        listaVisiveis.addAll(novaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_oferta, parent, false)
        return OfertaViewHolder(view)
    }
    override fun onBindViewHolder(holder: OfertaViewHolder, position: Int) {
        val oferta = listaVisiveis[position]

        // 1. Dados de Texto
        holder.txtNome.text      = oferta.nome ?: "Oferta"
        holder.txtDescricao.text = "🏬 ${oferta.nomeSupermercado ?: "Supermercado"}"
        holder.txtDetalhesOferta.visibility = View.GONE

        // 2. Distância (Calculada via Localização)
        if (oferta.distancia < Double.MAX_VALUE && oferta.distancia > 0.0) {
            val df = DecimalFormat("#.#")
            holder.txtDistancia.text = "${df.format(oferta.distancia)} km"
            holder.txtDistancia.visibility = View.VISIBLE
        } else {
            holder.txtDistancia.visibility = View.GONE
        }

        // 3. Imagem (MUDANÇA: Use imagemOferta para mostrar a promoção em vez da logo)
        holder.imgOferta.loadImage(oferta.imagemOferta, R.drawable.logo_supermercado_placeholder)

        // 4. Coração (Favoritos - use isSaved do seu modelo)
        holder.imageViewSave.setImageResource(
            if (oferta.isSaved) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
        )

        // 5. Cliques
        holder.imageViewSave.setOnClickListener {
            // Envia o clique para a ListaOfertas tratar o salvamento no MongoDB
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                listener?.onSaveClick(oferta, currentPosition)
            }
        }

        holder.itemView.setOnClickListener {
            // Envia o clique para a ListaOfertas usar o DataHolder e abrir o VerPDF
            listener?.onOfertaClick(oferta)
        }
    }


    override fun getItemCount(): Int = listaVisiveis.size

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val filtradas: MutableList<Oferta> = ArrayList()
            val query = constraint.toString().lowercase(Locale.getDefault()).trim()

            if (query.isEmpty()) {
                filtradas.addAll(listaCompleta)
            } else {
                for (o in listaCompleta) {
                    val matchSuper = o.nomeSupermercado?.lowercase()?.contains(query) == true
                    val matchNome  = o.nome?.lowercase()?.contains(query) == true
                    if (matchSuper || matchNome) filtradas.add(o)
                }
            }
            return FilterResults().apply { values = filtradas }
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            val temp = (results.values as? List<*> ?: emptyList<Any>())
                .filterIsInstance<Oferta>()
                .toMutableList()

            // ── 1. Cálculo de Distância ───────────────────────────────────────
            val locUsuario = ultimaLocalizacao
            temp.forEach { oferta ->
                oferta.distancia = if (locUsuario != null && oferta.latitude != null && oferta.longitude != null) {
                    val locMercado = Location("").apply {
                        latitude = oferta.latitude!!
                        longitude = oferta.longitude!!
                    }
                    locUsuario.distanceTo(locMercado) / 1000.0
                } else {
                    Double.MAX_VALUE
                }
            }

            // ── 2. Ordenação ──────────────────────────────────────────────────
            temp.sortWith(compareByDescending<Oferta> { it.isSaved }.thenBy { it.distancia })

            listaVisiveis.clear()
            listaVisiveis.addAll(temp)
            notifyDataSetChanged()
        }
    }

    class OfertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgOferta: ImageView        = itemView.findViewById(R.id.imgOferta)
        val txtNome: TextView           = itemView.findViewById(R.id.txtNome)
        val txtDescricao: TextView      = itemView.findViewById(R.id.txtDescricao)
        val txtDistancia: TextView      = itemView.findViewById(R.id.txtDistancia)
        val txtDetalhesOferta: TextView = itemView.findViewById(R.id.txtDetalhesOferta)
        val imageViewSave: ImageView    = itemView.findViewById(R.id.imageViewSave)
    }
}
