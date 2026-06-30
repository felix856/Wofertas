package com.example.wofertas.ui.encartes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wofertas.R
import com.example.wofertas.network.EncarteDto

class EncartesAdapter(
    private val onVerClick: (EncarteDto) -> Unit,
    private val onEditClick: ((EncarteDto) -> Unit)? = null,
    private val onDeleteClick: ((EncarteDto) -> Unit)? = null
) : ListAdapter<EncarteDto, EncartesAdapter.EncarteViewHolder>(DIFF) {

    inner class EncarteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvEncarteTitulo)
        val tvData: TextView = itemView.findViewById(R.id.tvEncarteData)
        val btnVer: View = itemView.findViewById(R.id.btnVerEncarte)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditEncarte)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteEncarte)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EncarteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_encarte, parent, false)
        return EncarteViewHolder(view)
    }

    override fun onBindViewHolder(holder: EncarteViewHolder, position: Int) {
        val encarte = getItem(position)
        holder.tvTitulo.text = encarte.titulo
        holder.tvData.text = encarte.dataCriacao.take(10)
        holder.btnVer.setOnClickListener { onVerClick(encarte) }

        if (onEditClick != null) {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener { onEditClick.invoke(encarte) }
        } else {
            holder.btnEdit.visibility = View.GONE
            holder.btnEdit.setOnClickListener(null)
        }

        if (onDeleteClick != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDeleteClick.invoke(encarte) }
        } else {
            holder.btnDelete.visibility = View.GONE
            holder.btnDelete.setOnClickListener(null)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EncarteDto>() {
            override fun areItemsTheSame(a: EncarteDto, b: EncarteDto) = a.id == b.id
            override fun areContentsTheSame(a: EncarteDto, b: EncarteDto) = a == b
        }
    }
}
