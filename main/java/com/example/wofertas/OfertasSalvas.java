// app/src/main/java/com/example/wofertas/OfertasSalvas.java
package com.example.wofertas;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Importação explícita para ClienteOfertaAdapter e suas interfaces
import com.example.wofertas.ClienteOfertaAdapter;
import com.example.wofertas.ClienteOfertaAdapter.OnOfertaClickListener;
import com.example.wofertas.ClienteOfertaAdapter.OnSaveClickListener;


public class OfertasSalvas extends AppCompatActivity
        implements OnOfertaClickListener, OnSaveClickListener { // Usando as interfaces do ClienteOfertaAdapter

    private static final String TAG = "OfertasSalvasActivity";

    // Componentes da UI
    private RecyclerView recyclerView;
    private ClienteOfertaAdapter adapter; // Usando o ClienteOfertaAdapter
    private List<Oferta> listaOfertasSalvas;
    private ProgressBar progressBar;
    private TextView tvNoSavedOffersMessage;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private DocumentReference usuarioDocRef;
    private ListenerRegistration usuarioPerfilListener; // Listener para perfil do usuário

    private Location userLocation; // Você precisará obter esta localização como em ListaOfertas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ofertas_salvas);

        toolbar = findViewById(R.id.toolbar_ofertas_salvas);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ofertas Salvas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.recyclerViewOfertasSalvas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaOfertasSalvas = new ArrayList<>();
        // Instanciando o ClienteOfertaAdapter e passando 'this' como listeners
        adapter = new ClienteOfertaAdapter(this, listaOfertasSalvas, this, this);
        recyclerView.setAdapter(adapter);

        progressBar = findViewById(R.id.progressBarOfertasSalvas);
        tvNoSavedOffersMessage = findViewById(R.id.tvNoSavedOffersMessage);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Você não está logado. Faça login para ver ofertas salvas.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        usuarioDocRef = db.collection("usuarios").document(currentUser.getUid());
        setupUsuarioPerfilListener(); // Configura o listener para o perfil do usuário

        setupBottomNavigationView();

        // TODO: Implementar lógica para obter a localização do usuário aqui, se necessário para distância
        // Você pode reutilizar a lógica de LocationUtils e FusedLocationProviderClient de ListaOfertas.
        // Por enquanto, userLocation será nulo, e a distância não será calculada.
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega as ofertas salvas toda vez que a Activity volta ao foco
        carregarOfertasSalvas();
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
     * Configura o listener para o perfil do usuário para obter a lista de supermercados salvos.
     */
    private void setupUsuarioPerfilListener() {
        if (usuarioDocRef != null) {
            usuarioPerfilListener = usuarioDocRef.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Erro ao ouvir alterações no perfil do usuário.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    // Recarrega as ofertas salvas para refletir as mudanças
                    carregarOfertasSalvas();
                }
            });
        }
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_salvos); // Define "Salvos" como selecionado
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_ofertas) {
                startActivity(new Intent(OfertasSalvas.this, ListaOfertas.class));
                finish();
                return true;
            } else if (id == R.id.navigation_salvos) {
                Toast.makeText(OfertasSalvas.this, "Você já está em Ofertas Salvas.", Toast.LENGTH_SHORT).show();
                return true; // Já estamos nesta Activity
            } else if (id == R.id.navigation_mapa) {
                Intent intent = new Intent(OfertasSalvas.this, Mapa.class);
                // Certifique-se de que adapter.getListaCompleta() retorna uma ArrayList<Oferta>
                // ou converta-a antes de passar
                if (adapter != null && adapter.getListaCompleta() != null) {
                    intent.putParcelableArrayListExtra("ofertasParaMapa", new ArrayList<>(adapter.getListaCompleta()));
                } else {
                    intent.putParcelableArrayListExtra("ofertasParaMapa", new ArrayList<>());
                }
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_perfil) {
                startActivity(new Intent(OfertasSalvas.this, Perfil.class));
                finish();
                return true;
            }
            return false;
        });
    }

    /**
     * Carrega as ofertas salvas pelo usuário logado.
     * Filtra por status "ativa" e pelos IDs de supermercados salvos.
     */
    private void carregarOfertasSalvas() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoSavedOffersMessage.setVisibility(View.GONE);

        if (currentUser == null) {
            progressBar.setVisibility(View.GONE);
            tvNoSavedOffersMessage.setVisibility(View.VISIBLE);
            return;
        }

        usuarioDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    Usuario usuario = documentSnapshot.toObject(Usuario.class);
                    if (usuario != null && usuario.getSupermercadosSalvos() != null && !usuario.getSupermercadosSalvos().isEmpty()) {
                        List<String> supermercadosSalvosIds = usuario.getSupermercadosSalvos();
                        Log.d(TAG, "Supermercados salvos pelo usuário: " + supermercadosSalvosIds.size());

                        db.collection("ofertas")
                                .whereEqualTo("status", "ativa") // Apenas ofertas ativas
                                .whereIn("supermercadoId", supermercadosSalvosIds) // Filtra pelos IDs salvos
                                .orderBy("timestamp", Query.Direction.DESCENDING) // Ordena por mais recente
                                .get()
                                .addOnCompleteListener(task -> {
                                    progressBar.setVisibility(View.GONE);
                                    if (task.isSuccessful()) {
                                        listaOfertasSalvas.clear();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Oferta oferta = document.toObject(Oferta.class);
                                            oferta.setOfertaId(document.getId());
                                            oferta.setSaved(true); // Marca como salva
                                            // Calcula a distância se a localização do usuário e do supermercado estiverem disponíveis
                                            if (userLocation != null && oferta.getLatitude() != null && oferta.getLongitude() != null) {
                                                float[] results = new float[1];
                                                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                                                        oferta.getLatitude(), oferta.getLongitude(), results);
                                                oferta.setDistancia(results[0] / 1000.0); // Distância em km
                                            } else {
                                                oferta.setDistancia(-1.0); // Indica que a distância não está disponível
                                            }
                                            listaOfertasSalvas.add(oferta);
                                        }

                                        // Ordena por distância se a localização do usuário estiver disponível
                                        if (userLocation != null) {
                                            Collections.sort(listaOfertasSalvas, (o1, o2) -> {
                                                if (o1.getDistancia() != null && o2.getDistancia() != null && o1.getDistancia() >= 0 && o2.getDistancia() >= 0) {
                                                    return Double.compare(o1.getDistancia(), o2.getDistancia());
                                                }
                                                return 0;
                                            });
                                        }

                                        adapter.setOfertas(listaOfertasSalvas); // Atualiza o adaptador do Cliente
                                        if (listaOfertasSalvas.isEmpty()) {
                                            tvNoSavedOffersMessage.setVisibility(View.VISIBLE);
                                        } else {
                                            tvNoSavedOffersMessage.setVisibility(View.GONE);
                                        }
                                    } else {
                                        Log.w(TAG, "Erro ao carregar ofertas salvas.", task.getException());
                                        Toast.makeText(OfertasSalvas.this, "Erro ao carregar ofertas salvas.", Toast.LENGTH_SHORT).show();
                                        progressBar.setVisibility(View.GONE);
                                        tvNoSavedOffersMessage.setVisibility(View.VISIBLE);
                                    }
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        listaOfertasSalvas.clear();
                        adapter.setOfertas(listaOfertasSalvas); // Limpa a lista no adaptador
                        tvNoSavedOffersMessage.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Nenhum supermercado salvo encontrado para o usuário.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar dados do usuário para ofertas salvas: " + e.getMessage());
                    Toast.makeText(this, "Erro ao carregar dados do usuário.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    tvNoSavedOffersMessage.setVisibility(View.VISIBLE);
                });
    }

    @Override
    public void onOfertaClick(Oferta oferta) { // Implementação do clique no PDF
        if (oferta.getUrlPdf() != null && !oferta.getUrlPdf().isEmpty()) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(oferta.getUrlPdf()));
            startActivity(browserIntent);
        } else {
            Toast.makeText(this, "PDF da oferta não disponível.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveClick(Oferta oferta, int position) { // Implementação do salvar/desalvar
        if (currentUser == null) {
            Toast.makeText(this, "Faça login para gerenciar ofertas salvas.", Toast.LENGTH_SHORT).show();
            return;
        }

        String supermercadoId = oferta.getSupermercadoId();
        if (supermercadoId == null || supermercadoId.isEmpty()) {
            Toast.makeText(this, "ID do supermercado não encontrado para desalvar.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Tentativa de desalvar oferta com supermercadoId nulo/vazio.");
            return;
        }

        // Como estamos na tela de Salvas, a ação de clique no botão de salvar/desalvar
        // é sempre para REMOVER dos salvos.
        usuarioDocRef.update("supermercadosSalvos", FieldValue.arrayRemove(supermercadoId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Oferta removida dos salvos.", Toast.LENGTH_SHORT).show();
                    listaOfertasSalvas.remove(position); // Remove da lista local
                    adapter.notifyItemRemoved(position); // Notifica o adaptador
                    if (listaOfertasSalvas.isEmpty()) {
                        tvNoSavedOffersMessage.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao desalvar oferta.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Erro ao desalvar supermercado: " + e.getMessage());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove o listener do Firestore para evitar vazamentos de memória
        if (usuarioPerfilListener != null) {
            usuarioPerfilListener.remove();
        }
        // Se você implementar a lógica de localização aqui, pare as atualizações também.
    }
}
