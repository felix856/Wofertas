package com.example.wofertas;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OfertaAdapter extends RecyclerView.Adapter<OfertaAdapter.OfertaViewHolder> implements Filterable {

    private List<Oferta> listaOfertasVisiveis;
    private List<Oferta> listaOfertasCompleta;
    private final Context context;

    public OfertaAdapter(List<Oferta> ofertas, Context context) {
        this.listaOfertasVisiveis = ofertas;
        this.listaOfertasCompleta = new ArrayList<>(ofertas);
        this.context = context;
    }

    public void atualizarListaCompleta(List<Oferta> novaLista) {
        this.listaOfertasCompleta = new ArrayList<>(novaLista);
        // Aplica o filtro atual à nova lista (se houver um) ou simplesmente reseta
        getFilter().filter(null);
    }

    @NonNull
    @Override
    public OfertaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_oferta, parent, false);
        return new OfertaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfertaViewHolder holder, int position) {
        Oferta oferta = listaOfertasVisiveis.get(position);

        holder.txtNome.setText(oferta.getNome());
        holder.txtDescricao.setText(oferta.getDescricao());

        if (oferta.getDistancia() > 0 && oferta.getDistancia() != Double.MAX_VALUE) {
            holder.txtDistancia.setText(String.format(Locale.getDefault(), "%.1f km", oferta.getDistancia() / 1000.0));
            holder.txtDistancia.setVisibility(View.VISIBLE);
        } else {
            holder.txtDistancia.setVisibility(View.GONE);
        }

        if (oferta.getThumbUrl() != null && !oferta.getThumbUrl().isEmpty()) {
            Glide.with(context)
                    .load(oferta.getThumbUrl())
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.imgOferta);
        } else {
            holder.imgOferta.setImageResource(R.drawable.ic_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            String pdfUrl = oferta.getUrlPdf();
            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                Intent intent = new Intent(context, VerPDF.class);
                intent.putExtra("oferta_pdf_url", pdfUrl);
                intent.putExtra("oferta_nome", oferta.getNome() + " - " + oferta.getDescricao());
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "PDF indisponível", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaOfertasVisiveis.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Oferta> filtradas = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtradas.addAll(listaOfertasCompleta);
                } else {
                    String filtroPadrao = constraint.toString().toLowerCase().trim();
                    for (Oferta o : listaOfertasCompleta) {
                        boolean nomeContem = o.getNome() != null && o.getNome().toLowerCase().contains(filtroPadrao);
                        boolean descricaoContem = o.getDescricao() != null && o.getDescricao().toLowerCase().contains(filtroPadrao);
                        if (nomeContem || descricaoContem) {
                            filtradas.add(o);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtradas;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                listaOfertasVisiveis.clear();
                if (results.values instanceof List) {
                    listaOfertasVisiveis.addAll((List<Oferta>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }

    static class OfertaViewHolder extends RecyclerView.ViewHolder {
        ImageView imgOferta;
        TextView txtNome, txtDescricao, txtDistancia;

        public OfertaViewHolder(@NonNull View itemView) {
            super(itemView);
            imgOferta = itemView.findViewById(R.id.imgOferta);
            txtNome = itemView.findViewById(R.id.txtNome);
            txtDescricao = itemView.findViewById(R.id.txtDescricao);
            txtDistancia = itemView.findViewById(R.id.txtDistancia);
        }
    }
}
