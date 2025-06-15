package com.example.wofertas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListaOfertas extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OfertaAdapter ofertaAdapter;
    private List<Oferta> listaOfertas = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration ofertasListener;
    private FusedLocationProviderClient fusedLocationClient;
    private Location ultimaLocalizacaoUsuario;

    private SearchView searchView;
    private BottomNavigationView bottomNavigationView;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    obterLocalizacaoEOrdenarLista();
                } else {
                    Toast.makeText(this, "Permissão negada. Ofertas não serão ordenadas por distância.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_ofertas);

        Toolbar toolbar = findViewById(R.id.toolbar_lista_ofertas);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        searchView = findViewById(R.id.searchView);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ofertaAdapter = new OfertaAdapter(listaOfertas, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(ofertaAdapter);

        configurarBusca();
        configurarNavegacaoInferior();
        recuperarOfertas();
        verificarPermissaoDeLocalizacao();
    }

    private void configurarBusca() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (ofertaAdapter != null) {
                    ofertaAdapter.getFilter().filter(newText);
                }
                return false;
            }
        });
    }

    private void configurarNavegacaoInferior() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                return true;
            } else if (itemId == R.id.navigation_mapa) {
                Intent intent = new Intent(this, Mapa.class);
                intent.putExtra("lista_ofertas", new ArrayList<>(listaOfertas));
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_salvos) {
                Toast.makeText(this, "Funcionalidade 'Salvos' em desenvolvimento.", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void verificarPermissaoDeLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obterLocalizacaoEOrdenarLista();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void obterLocalizacaoEOrdenarLista() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            ultimaLocalizacaoUsuario = location;
                            ordenarOfertasPorDistancia();
                        }
                    });
        } catch (SecurityException e) {
            Log.e("SecurityException", "Permissão de localização revogada.", e);
        }
    }

    private void recuperarOfertas() {
        CollectionReference ofertasRef = db.collection("ofertas");
        ofertasListener = ofertasRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("FIRESTORE_ERROR", "Listen failed.", e);
                        Toast.makeText(ListaOfertas.this, "Erro ao carregar ofertas.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Oferta> novasOfertas = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            if (doc.contains("latitude") && doc.getDouble("latitude") != null &&
                                    doc.contains("longitude") && doc.getDouble("longitude") != null) {
                                Oferta oferta = doc.toObject(Oferta.class);
                                if (oferta != null) {
                                    oferta.setOfertaId(doc.getId());
                                    novasOfertas.add(oferta);
                                }
                            }
                        }
                    }
                    listaOfertas.clear();
                    listaOfertas.addAll(novasOfertas);
                    ofertaAdapter.atualizarListaCompleta(novasOfertas);
                    ordenarOfertasPorDistancia();
                });
    }

    private void ordenarOfertasPorDistancia() {
        if (ultimaLocalizacaoUsuario != null && !listaOfertas.isEmpty()) {
            for (Oferta oferta : listaOfertas) {
                if (oferta.getLatitude() != null && oferta.getLongitude() != null) {
                    Location localSupermercado = new Location("");
                    localSupermercado.setLatitude(oferta.getLatitude());
                    localSupermercado.setLongitude(oferta.getLongitude());
                    float distanciaEmMetros = ultimaLocalizacaoUsuario.distanceTo(localSupermercado);
                    oferta.setDistancia(distanciaEmMetros);
                } else {
                    oferta.setDistancia(Double.MAX_VALUE);
                }
            }
            Collections.sort(listaOfertas, Comparator.comparingDouble(Oferta::getDistancia));
        }
        if (ofertaAdapter != null) {
            ofertaAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.lista_ofertas_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_perfil) {
            startActivity(new Intent(this, Perfil.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ofertasListener != null) {
            ofertasListener.remove();
        }
    }
}
