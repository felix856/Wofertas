// app/src/main/java/com/example/wofertas/GerenciarOfertasSupermercadoActivity.java
package com.example.wofertas;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class GerenciarOfertasSupermercadoActivity extends AppCompatActivity
        implements SupermercadoOfertaAdapter.OnItemActionListener { // Implementa a interface do SupermercadoOfertaAdapter

    private static final String TAG = "GerenciarOfertasSuper";

    private RecyclerView recyclerView;
    private SupermercadoOfertaAdapter adapter; // Usando o SupermercadoOfertaAdapter
    private List<Oferta> listaOfertas;
    private ProgressBar progressBar;
    private TextView tvNoOffersMessageSupermercado; // Mensagem de "nenhuma oferta"
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private String urlLogoSupermercado; // Para armazenar a URL da logo do supermercado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_ofertas_supermercado);

        toolbar = findViewById(R.id.toolbar_gerenciar_ofertas);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Minhas Ofertas"); // Título da tela
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.recyclerViewGerenciarOfertas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaOfertas = new ArrayList<>();
        // Instanciando o SupermercadoOfertaAdapter e passando 'this' como listener
        adapter = new SupermercadoOfertaAdapter(this, listaOfertas, this);
        recyclerView.setAdapter(adapter);

        progressBar = findViewById(R.id.progressBarGerenciarOfertas);
        tvNoOffersMessageSupermercado = findViewById(R.id.tvNoOffersMessageSupermercado); // Inicializa a TextView
        bottomNavigationView = findViewById(R.id.bottom_navigation_supermercado);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado. Redirecionando para o login.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Buscar a URL da logo do supermercado ao iniciar a Activity
        buscarUrlLogoSupermercado();

        setupBottomNavigationView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega as ofertas toda vez que a Activity volta ao foco (ex: após editar/publicar)
        carregarMinhasOfertas();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Configura a navegação inferior (BottomNavigationView) para o perfil do supermercado.
     */
    private void setupBottomNavigationView() {
        // Define o item "Minhas Ofertas" como selecionado
        bottomNavigationView.setSelectedItemId(R.id.navigation_gerenciar_ofertas);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_publicar_oferta) {
                startActivity(new Intent(this, DashboardSupermercadoActivity.class));
                finish(); // Finaliza esta activity para não empilhar
                return true;
            } else if (id == R.id.navigation_gerenciar_ofertas) {
                Toast.makeText(this, "Você já está em Minhas Ofertas.", Toast.LENGTH_SHORT).show();
                return true; // Já está nesta Activity
            } else if (id == R.id.navigation_perfil_supermercado) {
                startActivity(new Intent(this, Perfil.class)); // Assumindo que você tem uma Perfil Activity
                finish(); // Finaliza esta activity para não empilhar
                return true;
            }
            return false;
        });
    }

    /**
     * Busca a URL da logo do supermercado logado.
     * Isso é necessário para a lógica de exclusão do thumbnail.
     */
    private void buscarUrlLogoSupermercado() {
        if (currentUser == null) return;

        db.collection("usuarios").document(currentUser.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Usuario usuario = snapshot.toObject(Usuario.class);
                        if (usuario != null && "supermercado".equalsIgnoreCase(usuario.getPerfil())) {
                            urlLogoSupermercado = usuario.getUrlLogo();
                            if (urlLogoSupermercado == null) {
                                urlLogoSupermercado = ""; // Garante que não seja nulo
                            }
                            Log.d(TAG, "URL da logo do supermercado carregada: " + urlLogoSupermercado);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar URL da logo do supermercado: " + e.getMessage());
                    urlLogoSupermercado = ""; // Em caso de erro, define como vazio
                });
    }

    /**
     * Carrega as ofertas publicadas pelo supermercado logado.
     * Não filtra por status para que o supermercado veja todas as suas ofertas (ativas, agendadas, inativas).
     */
    private void carregarMinhasOfertas() {
        if (currentUser == null) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvNoOffersMessageSupermercado.setVisibility(View.GONE); // Esconde a mensagem enquanto carrega

        db.collection("ofertas")
                .whereEqualTo("supermercadoId", currentUser.getUid()) // Filtra por ID do supermercado logado
                .orderBy("timestamp", Query.Direction.DESCENDING) // Ordena por mais recente
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        listaOfertas.clear();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                            Oferta oferta = document.toObject(Oferta.class);
                            oferta.setOfertaId(document.getId()); // Define o ID do documento da oferta
                            listaOfertas.add(oferta);
                        }
                        adapter.atualizarLista(listaOfertas); // Atualiza o adaptador com a nova lista
                        if (listaOfertas.isEmpty()) {
                            tvNoOffersMessageSupermercado.setVisibility(View.VISIBLE); // Mostra a mensagem se não houver ofertas
                        } else {
                            tvNoOffersMessageSupermercado.setVisibility(View.GONE);
                        }
                    } else {
                        Log.w(TAG, "Erro ao carregar minhas ofertas.", task.getException());
                        Toast.makeText(this, "Erro ao carregar suas ofertas.", Toast.LENGTH_SHORT).show();
                        tvNoOffersMessageSupermercado.setVisibility(View.VISIBLE); // Mostra a mensagem em caso de erro
                    }
                });
    }

    /**
     * Implementação da interface SupermercadoOfertaAdapter.OnItemActionListener
     * Lida com o clique no botão "Ver PDF".
     */
    @Override
    public void onVerPdfClick(Oferta oferta) { // <--- Este é o método correto a ser implementado
        if (oferta.getUrlPdf() != null && !oferta.getUrlPdf().isEmpty()) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(oferta.getUrlPdf()));
            startActivity(browserIntent);
        } else {
            Toast.makeText(this, "PDF da oferta não disponível.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Implementação da interface SupermercadoOfertaAdapter.OnItemActionListener
     * Lida com o clique no botão de editar oferta.
     */
    @Override
    public void onEditClick(Oferta oferta) {
        Toast.makeText(this, "Editando oferta: " + oferta.getDescricao(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DashboardSupermercadoActivity.class); // Redireciona para a tela de publicação/edição
        intent.putExtra("ofertaParaEditar", oferta); // Passa o objeto Oferta (Parcelable)
        startActivity(intent);
    }

    /**
     * Implementação da interface SupermercadoOfertaAdapter.OnItemActionListener
     * Lida com o clique no botão de excluir oferta.
     */
    @Override
    public void onDeleteClick(Oferta oferta, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Oferta")
                .setMessage("Tem certeza que deseja excluir a oferta '" + oferta.getDescricao() + "'?")
                .setPositiveButton("Excluir", (dialog, which) -> excluirOferta(oferta, position))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Exclui a oferta do Firestore e o PDF associado do Storage.
     * @param oferta A oferta a ser excluída.
     * @param position A posição da oferta na lista do adaptador.
     */
    private void excluirOferta(Oferta oferta, int position) {
        progressBar.setVisibility(View.VISIBLE);

        // 1. Excluir o PDF do Firebase Storage (se houver)
        if (oferta.getUrlPdf() != null && !oferta.getUrlPdf().isEmpty()) {
            try {
                StorageReference pdfRef = storage.getReferenceFromUrl(oferta.getUrlPdf());
                pdfRef.delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "PDF excluído do Storage: " + oferta.getUrlPdf()))
                        .addOnFailureListener(e -> Log.w(TAG, "Falha ao excluir PDF do Storage: " + e.getMessage()));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "URL do PDF inválida para exclusão: " + oferta.getUrlPdf() + " - " + e.getMessage());
            }
        }

        // 2. Excluir o thumbnail (logo do supermercado) do Firebase Storage (se houver)
        // Só exclua a thumbnail se ela NÃO for a URL da logo padrão do supermercado.
        // Isso evita deletar a logo principal do supermercado se ela estiver sendo usada como thumbnail.
        if (oferta.getThumbUrl() != null && !oferta.getThumbUrl().isEmpty() && !oferta.getThumbUrl().equals(urlLogoSupermercado)) {
            try {
                StorageReference thumbRef = storage.getReferenceFromUrl(oferta.getThumbUrl());
                thumbRef.delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Miniatura excluída do Storage com sucesso: " + oferta.getThumbUrl()))
                        .addOnFailureListener(e -> Log.e(TAG, "Erro ao excluir miniatura do Storage: " + e.getMessage()));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "URL inválida para StorageReference (Thumb): " + oferta.getThumbUrl() + " - " + e.getMessage());
            }
        }

        // 3. Excluir o documento da oferta do Firestore
        db.collection("ofertas").document(oferta.getOfertaId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Oferta excluída com sucesso!", Toast.LENGTH_SHORT).show();
                    listaOfertas.remove(position); // Remove da lista local
                    adapter.notifyItemRemoved(position); // Notifica o adaptador
                    progressBar.setVisibility(View.GONE);
                    if (listaOfertas.isEmpty()) { // Verifica se a lista ficou vazia após a exclusão
                        tvNoOffersMessageSupermercado.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erro ao excluir oferta do Firestore.", e);
                    Toast.makeText(this, "Erro ao excluir oferta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
