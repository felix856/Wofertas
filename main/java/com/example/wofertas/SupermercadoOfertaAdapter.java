// app/src/main/java/com/example/wofertas/SupermercadoOfertaAdapter.java
package com.example.wofertas;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter para exibir e gerenciar as ofertas publicadas por um supermercado.
 * Exibe status, e botões de editar/excluir.
 */
public class SupermercadoOfertaAdapter extends RecyclerView.Adapter<SupermercadoOfertaAdapter.OfertaSupermercadoViewHolder> {

    private static final String TAG = "SupermercadoOfertaAdapt";
    private Context context;
    private List<Oferta> listaOfertas;
    private OnItemActionListener listener;

    public SupermercadoOfertaAdapter(Context context, List<Oferta> listaOfertas, OnItemActionListener listener) {
        this.context = context;
        this.listaOfertas = listaOfertas;
        this.listener = listener;
    }

    // Interface para lidar com as ações nos itens da oferta (Ver PDF, Editar, Excluir)
    public interface OnItemActionListener {
        void onVerPdfClick(Oferta oferta); // <--- Este é o método correto para ver PDF
        void onEditClick(Oferta oferta);
        void onDeleteClick(Oferta oferta, int position);
    }

    @NonNull
    @Override
    public OfertaSupermercadoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla o layout específico para o supermercado
        View view = LayoutInflater.from(context).inflate(R.layout.item_oferta_supermercado, parent, false);
        return new OfertaSupermercadoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfertaSupermercadoViewHolder holder, int position) {
        Oferta oferta = listaOfertas.get(position);

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

        // Exibe o status da oferta e a data/hora de agendamento
        String statusText = "Status: ";
        if (oferta.getStatus() != null) {
            statusText += capitalizeFirstLetter(oferta.getStatus()); // Capitaliza a primeira letra
            if ("agendada".equalsIgnoreCase(oferta.getStatus()) && oferta.getScheduledAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                statusText += " para " + sdf.format(oferta.getScheduledAt().toDate());
            } else if ("inativa".equalsIgnoreCase(oferta.getStatus())) {
                // Você pode adicionar mais detalhes para o status inativo se houver
            }
        } else {
            statusText += "Não Definido"; // Caso o status seja nulo
        }
        holder.tvStatusOferta.setText(statusText);


        // Configura listeners para os botões de ação
        holder.btnVerPdf.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVerPdfClick(oferta); // <--- Chamando o método correto
            }
        });

        holder.btnEditarOferta.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(oferta);
            }
        });

        holder.btnExcluirOferta.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(oferta, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaOfertas.size();
    }

    /**
     * Atualiza a lista de ofertas e notifica o RecyclerView para redesenhar.
     * @param novasOfertas A nova lista de ofertas para exibir.
     */
    public void atualizarLista(List<Oferta> novasOfertas) {
        this.listaOfertas = novasOfertas;
        notifyDataSetChanged();
    }

    // Método auxiliar para capitalizar a primeira letra de uma string
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }


    static class OfertaSupermercadoViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageViewSupermercadoLogo;
        TextView tvNomeSupermercado, tvDescricaoOferta, tvDetalhesOferta, tvStatusOferta;
        Button btnVerPdf, btnEditarOferta, btnExcluirOferta;

        public OfertaSupermercadoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewSupermercadoLogo = itemView.findViewById(R.id.imageViewSupermercadoLogo);
            tvNomeSupermercado = itemView.findViewById(R.id.tvNomeSupermercado);
            tvDescricaoOferta = itemView.findViewById(R.id.tvDescricaoOferta);
            tvDetalhesOferta = itemView.findViewById(R.id.tvDetalhesOferta);
            tvStatusOferta = itemView.findViewById(R.id.tvStatusOferta);
            btnVerPdf = itemView.findViewById(R.id.btnVerPdf);
            btnEditarOferta = itemView.findViewById(R.id.btnEditarOferta);
            btnExcluirOferta = itemView.findViewById(R.id.btnExcluirOferta);
        }
    }
}
