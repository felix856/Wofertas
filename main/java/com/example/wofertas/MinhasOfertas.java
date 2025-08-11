// app/src/main/java/com/example/wofertas/MinhasOfertas.java
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

public class MinhasOfertas extends AppCompatActivity
        implements SupermercadoOfertaAdapter.OnItemActionListener { // <--- Implementa a interface correta

    private static final String TAG = "MinhasOfertas";

    private RecyclerView recyclerView;
    private SupermercadoOfertaAdapter adapter; // <--- Usando o SupermercadoOfertaAdapter
    private List<Oferta> listaOfertas;
    private ProgressBar progressBar;
    private TextView tvNoOffersMessage; // Mensagem de "nenhuma oferta"
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minhas_ofertas); // Certifique-se de ter este layout

        toolbar = findViewById(R.id.toolbar_minhas_ofertas); // ID do toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Minhas Ofertas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.recyclerViewMinhasOfertas); // ID do RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaOfertas = new ArrayList<>();
        // Instanciando o SupermercadoOfertaAdapter e passando 'this' como listener
        adapter = new SupermercadoOfertaAdapter(this, listaOfertas, this);
        recyclerView.setAdapter(adapter);

        progressBar = findViewById(R.id.progressBarMinhasOfertas); // ID do ProgressBar
        tvNoOffersMessage = findViewById(R.id.tvNoOffersMessage); // ID da TextView de mensagem
        bottomNavigationView = findViewById(R.id.bottom_navigation_supermercado); // ID da BottomNavigationView

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

        setupBottomNavigationView();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        bottomNavigationView.setSelectedItemId(R.id.navigation_gerenciar_ofertas); // Define este item como selecionado
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_publicar_oferta) {
                startActivity(new Intent(this, DashboardSupermercadoActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_dashboard_supermercado) {
                // Se você tiver uma tela de "dashboard" mais geral, pode ir para ela
                startActivity(new Intent(this, GerenciarOfertasSupermercadoActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_gerenciar_ofertas) {
                Toast.makeText(this, "Você já está em Minhas Ofertas.", Toast.LENGTH_SHORT).show();
                return true; // Já está nesta Activity
            } else if (id == R.id.navigation_perfil_supermercado) {
                startActivity(new Intent(this, Perfil.class));
                finish();
                return true;
            }
            return false;
        });
    }

    /**
     * Carrega as ofertas publicadas pelo supermercado logado.
     */
    private void carregarMinhasOfertas() {
        if (currentUser == null) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvNoOffersMessage.setVisibility(View.GONE); // Esconde a mensagem enquanto carrega

        db.collection("ofertas")
                .whereEqualTo("supermercadoId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        listaOfertas.clear();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                            Oferta oferta = document.toObject(Oferta.class);
                            oferta.setOfertaId(document.getId());
                            listaOfertas.add(oferta);
                        }
                        adapter.atualizarLista(listaOfertas);
                        if (listaOfertas.isEmpty()) {
                            tvNoOffersMessage.setVisibility(View.VISIBLE);
                        } else {
                            tvNoOffersMessage.setVisibility(View.GONE);
                        }
                    } else {
                        Log.w(TAG, "Erro ao carregar minhas ofertas.", task.getException());
                        Toast.makeText(this, "Erro ao carregar suas ofertas.", Toast.LENGTH_SHORT).show();
                        tvNoOffersMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    /**
     * Implementação da interface SupermercadoOfertaAdapter.OnItemActionListener
     * Lida com o clique no botão "Ver PDF".
     */
    @Override
    public void onVerPdfClick(Oferta oferta) {
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
        Intent intent = new Intent(this, DashboardSupermercadoActivity.class);
        intent.putExtra("ofertaParaEditar", oferta);
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

        // 2. Excluir o documento da oferta do Firestore
        db.collection("ofertas").document(oferta.getOfertaId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Oferta excluída com sucesso!", Toast.LENGTH_SHORT).show();
                    listaOfertas.remove(position);
                    adapter.notifyItemRemoved(position);
                    progressBar.setVisibility(View.GONE);
                    if (listaOfertas.isEmpty()) {
                        tvNoOffersMessage.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erro ao excluir oferta do Firestore.", e);
                    Toast.makeText(this, "Erro ao excluir oferta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
}
