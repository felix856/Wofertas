// app/src/main/java/com/example/wofertas/ClienteOfertaAdapter.java
package com.example.wofertas;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter para exibir a lista de ofertas para o cliente.
 * Gerencia a exibição dos dados, carregamento de imagens, distância e filtragem.
 */
public class ClienteOfertaAdapter extends RecyclerView.Adapter<ClienteOfertaAdapter.OfertaViewHolder> implements Filterable {

    private List<Oferta> listaOfertasVisiveis;
    private List<Oferta> listaOfertasCompletaOriginal;
    private final OnOfertaClickListener listener;
    private final OnSaveClickListener saveListener; // Novo listener para salvar/desalvar
    private final Context context;
    private Location ultimaLocalizacaoUsuario;

    public ClienteOfertaAdapter(Context context, List<Oferta> listaOfertas, OnOfertaClickListener listener, OnSaveClickListener saveListener) {
        this.context = context;
        this.listaOfertasVisiveis = listaOfertas;
        this.listaOfertasCompletaOriginal = new ArrayList<>(listaOfertas);
        this.listener = listener;
        this.saveListener = saveListener;
    }

    public interface OnOfertaClickListener {
        void onOfertaClick(Oferta oferta); // Para ver PDF
    }

    public interface OnSaveClickListener {
        void onSaveClick(Oferta oferta, int position); // Para salvar/desalvar
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

        holder.tvNomeSupermercado.setText(oferta.getNomeSupermercado());
        holder.tvDescricaoOferta.setText(oferta.getDescricao());
        holder.tvDetalhesOferta.setText(oferta.getDetalhesOferta());

        // Carrega a imagem de perfil/logo do supermercado
        if (!TextUtils.isEmpty(oferta.getThumbUrl())) {
            Glide.with(context)
                    .load(oferta.getThumbUrl())
                    .placeholder(R.drawable.logo_supermercado_placeholder)
                    .error(R.drawable.logo_supermercado_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imageViewSupermercadoLogo);
            holder.imageViewSupermercadoLogo.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewSupermercadoLogo.setImageResource(R.drawable.logo_supermercado_placeholder);
            holder.imageViewSupermercadoLogo.setVisibility(View.VISIBLE);
        }

        // Exibe a distância se disponível
        if (oferta.getDistancia() != null && oferta.getDistancia() >= 0) {
            DecimalFormat df = new DecimalFormat("#.##");
            holder.tvDistancia.setText(df.format(oferta.getDistancia()) + " km de você");
            holder.tvDistancia.setVisibility(View.VISIBLE);
        } else {
            holder.tvDistancia.setVisibility(View.GONE);
        }

        // Configura o botão Ver PDF
        holder.btnVerPdf.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOfertaClick(oferta);
            }
        });

        // Configura o botão Salvar/Desalvar
        if (oferta.isSaved()) {
            holder.btnSalvarOferta.setImageResource(R.drawable.ic_heart_filled);
        } else {
            holder.btnSalvarOferta.setImageResource(R.drawable.ic_heart_empty);
        }
        holder.btnSalvarOferta.setOnClickListener(v -> {
            if (saveListener != null) {
                saveListener.onSaveClick(oferta, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaOfertasVisiveis.size();
    }

    /**
     * Atualiza a lista de ofertas e notifica o RecyclerView para redesenhar.
     * @param novasOfertas A nova lista de ofertas para exibir.
     */
    public void setOfertas(List<Oferta> novasOfertas) {
        this.listaOfertasVisiveis = novasOfertas;
        this.listaOfertasCompletaOriginal = new ArrayList<>(novasOfertas);
        notifyDataSetChanged();
    }

    /**
     * Atualiza a localização do usuário e recalcula as distâncias.
     * @param location A última localização conhecida do usuário.
     */
    public void setUltimaLocalizacaoUsuario(Location location) {
        this.ultimaLocalizacaoUsuario = location;
        for (Oferta oferta : listaOfertasVisiveis) {
            if (oferta.getLatitude() != null && oferta.getLongitude() != null && location != null) {
                float[] results = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                        oferta.getLatitude(), oferta.getLongitude(), results);
                oferta.setDistancia(results[0] / 1000.0); // Distância em km
            } else {
                oferta.setDistancia(-1.0); // Indica que a distância não está disponível
            }
        }
        // Opcional: Reordenar por distância após atualização da localização
        Collections.sort(listaOfertasVisiveis, (o1, o2) -> {
            if (o1.getDistancia() != null && o2.getDistancia() != null && o1.getDistancia() >= 0 && o2.getDistancia() >= 0) {
                return Double.compare(o1.getDistancia(), o2.getDistancia());
            }
            return 0;
        });
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString().toLowerCase(Locale.getDefault());
                List<Oferta> filteredList = new ArrayList<>();

                if (charString.isEmpty()) {
                    filteredList.addAll(listaOfertasCompletaOriginal);
                } else {
                    for (Oferta oferta : listaOfertasCompletaOriginal) {
                        if (oferta.getDescricao().toLowerCase(Locale.getDefault()).contains(charString) ||
                                oferta.getNomeSupermercado().toLowerCase(Locale.getDefault()).contains(charString) ||
                                (oferta.getDetalhesOferta() != null && oferta.getDetalhesOferta().toLowerCase(Locale.getDefault()).contains(charString))) {
                            filteredList.add(oferta);
                        }
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                listaOfertasVisiveis.clear();
                listaOfertasVisiveis.addAll((List<Oferta>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    public List<Oferta> getListaCompleta() {
        return listaOfertasCompletaOriginal; // Retorna a lista completa para o mapa
    }


    static class OfertaViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageViewSupermercadoLogo;
        TextView tvNomeSupermercado, tvDescricaoOferta, tvDetalhesOferta, tvDistancia;
        Button btnVerPdf;
        ImageButton btnSalvarOferta;

        public OfertaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewSupermercadoLogo = itemView.findViewById(R.id.imageViewSupermercadoLogo);
            tvNomeSupermercado = itemView.findViewById(R.id.tvNomeSupermercado);
            tvDescricaoOferta = itemView.findViewById(R.id.tvDescricaoOferta);
            tvDetalhesOferta = itemView.findViewById(R.id.tvDetalhesOferta);
            tvDistancia = itemView.findViewById(R.id.tvDistancia);
            btnVerPdf = itemView.findViewById(R.id.btnVerPdf);
            btnSalvarOferta = itemView.findViewById(R.id.btnSalvarOferta);
        }
    }
}
