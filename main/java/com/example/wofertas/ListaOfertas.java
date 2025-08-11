// app/src/main/java/com/example/wofertas/ListaOfertas.java
package com.example.wofertas;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Importações explícitas para ClienteOfertaAdapter e suas interfaces
import com.example.wofertas.ClienteOfertaAdapter.OnOfertaClickListener;
import com.example.wofertas.ClienteOfertaAdapter.OnSaveClickListener;


public class ListaOfertas extends AppCompatActivity
        implements OnOfertaClickListener, OnSaveClickListener { // Usando as interfaces do ClienteOfertaAdapter

    private RecyclerView recyclerView;
    private ClienteOfertaAdapter adapter; // Usando o ClienteOfertaAdapter
    private List<Oferta> listaOfertas;
    private ProgressBar progressBar;
    private SearchView searchView;
    private TextView tvListaOfertasMessage;
    private Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DocumentReference usuarioDocRef;
    private ListenerRegistration ofertasListener;
    private ListenerRegistration usuarioPerfilListener;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location userLocation;
    private static final int REQUEST_CHECK_SETTINGS = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;

    private static final double MAX_DISTANCE_KM = 500.0; // Distância máxima para exibir ofertas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_ofertas);

        toolbar = findViewById(R.id.toolbar_lista_ofertas);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ofertas Próximas");
        }

        recyclerView = findViewById(R.id.recyclerView); // Verifique se o ID está correto no seu XML
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaOfertas = new ArrayList<>();
        // Instanciando o ClienteOfertaAdapter e passando 'this' como listeners
        adapter = new ClienteOfertaAdapter(this, listaOfertas, this, this);
        recyclerView.setAdapter(adapter);

        progressBar = findViewById(R.id.progressBarListaOfertas);
        searchView = findViewById(R.id.searchView);
        tvListaOfertasMessage = findViewById(R.id.tvListaOfertasMessage);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            usuarioDocRef = db.collection("usuarios").document(currentUser.getUid());
            setupUsuarioPerfilListener(); // Configura o listener para o perfil do usuário
        }

        setupSearchView();
        setupBottomNavigationView();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();
        createLocationCallback();

        checkLocationPermissionsAndSettings();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_ofertas);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_ofertas) {
                return true;
            } else if (id == R.id.navigation_salvos) {
                startActivity(new Intent(ListaOfertas.this, OfertasSalvas.class));
                finish();
                return true;
            } else if (id == R.id.navigation_mapa) {
                Intent intent = new Intent(ListaOfertas.this, Mapa.class);
                if (adapter != null && adapter.getListaCompleta() != null) {
                    intent.putParcelableArrayListExtra("ofertasParaMapa", new ArrayList<>(adapter.getListaCompleta()));
                } else {
                    intent.putParcelableArrayListExtra("ofertasParaMapa", new ArrayList<>());
                }
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_perfil) {
                startActivity(new Intent(ListaOfertas.this, Perfil.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        userLocation = location;
                        Log.d(TAG, "Localização do usuário atualizada: " + userLocation.getLatitude() + ", " + userLocation.getLongitude());
                        adapter.setUltimaLocalizacaoUsuario(userLocation);
                        carregarOfertas();
                        fusedLocationClient.removeLocationUpdates(this);
                        break;
                    }
                }
            }
        };
    }

    private void checkLocationPermissionsAndSettings() {
        if (LocationUtils.checkLocationPermissions(this)) {
            checkLocationSettings();
        } else {
            LocationUtils.requestLocationPermissions(this, REQUEST_LOCATION_PERMISSION);
        }
    }

    private void checkLocationSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build());

        task.addOnCompleteListener(task1 -> {
            try {
                LocationSettingsResponse response = task1.getResult(ApiException.class);
                startLocationUpdates();
            } catch (ApiException exception) {
                switch (exception.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            resolvable.startResolutionForResult(ListaOfertas.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "PendingIntent unable to execute request.", e);
                        } catch (ClassCastException e) {
                            Log.e(TAG, "Cannot cast to ResolvableApiException.", e);
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        String errorMessage = "As configurações de localização não estão disponíveis. O dispositivo não tem sensores de localização ou a localização está desabilitada nas configurações.";
                        Log.e(TAG, errorMessage);
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        carregarOfertas();
                        break;
                }
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Iniciando atualizações de localização.");
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Parando atualizações de localização.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationPermissionsAndSettings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Configurações de localização ativadas pelo usuário.");
                startLocationUpdates();
            } else {
                Log.d(TAG, "Configurações de localização não ativadas pelo usuário.");
                Toast.makeText(this, "Localização é necessária para ver ofertas por distância.", Toast.LENGTH_LONG).show();
                carregarOfertas();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissão de localização concedida.");
                checkLocationSettings();
            } else {
                Log.d(TAG, "Permissão de localização negada.");
                Toast.makeText(this, "Permissão de localização negada. A distância das ofertas não será calculada.", Toast.LENGTH_LONG).show();
                carregarOfertas();
            }
        }
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
                    Usuario usuario = documentSnapshot.toObject(Usuario.class);
                    if (usuario != null && usuario.getSupermercadosSalvos() != null) {
                        carregarOfertas();
                    }
                }
            });
        }
    }

    /**
     * Carrega as ofertas do Firestore e as exibe no RecyclerView.
     * Filtra por status "ativa" para clientes.
     */
    private void carregarOfertas() {
        progressBar.setVisibility(View.VISIBLE);
        tvListaOfertasMessage.setVisibility(View.VISIBLE);

        db.collection("ofertas")
                .whereEqualTo("status", "ativa")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    tvListaOfertasMessage.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        List<Oferta> fetchedOfertas = new ArrayList<>();
                        List<String> supermercadosSalvos = new ArrayList<>();

                        if (currentUser != null && usuarioDocRef != null) {
                            usuarioDocRef.get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        Usuario usuario = documentSnapshot.toObject(Usuario.class);
                                        if (usuario != null && usuario.getSupermercadosSalvos() != null) {
                                            supermercadosSalvos.addAll(usuario.getSupermercadosSalvos());
                                        }
                                        processOfertas(task.getResult().getDocuments(), fetchedOfertas, supermercadosSalvos);
                                    }).addOnFailureListener(e -> {
                                        Log.e(TAG, "Erro ao buscar supermercados salvos do usuário: " + e.getMessage());
                                        processOfertas(task.getResult().getDocuments(), fetchedOfertas, supermercadosSalvos);
                                    });
                        } else {
                            processOfertas(task.getResult().getDocuments(), fetchedOfertas, supermercadosSalvos);
                        }

                    } else {
                        Log.w(TAG, "Erro ao carregar ofertas.", task.getException());
                        Toast.makeText(ListaOfertas.this, "Erro ao carregar ofertas.", Toast.LENGTH_SHORT).show();
                        tvListaOfertasMessage.setText("Erro ao carregar ofertas.");
                        tvListaOfertasMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void processOfertas(List<DocumentSnapshot> documents, List<Oferta> fetchedOfertas, List<String> supermercadosSalvos) {
        for (DocumentSnapshot document : documents) {
            Oferta oferta = document.toObject(Oferta.class);
            oferta.setOfertaId(document.getId());

            if (userLocation != null && oferta.getLatitude() != null && oferta.getLongitude() != null) {
                float[] results = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        oferta.getLatitude(), oferta.getLongitude(), results);
                double distanceKm = results[0] / 1000;

                if (distanceKm > MAX_DISTANCE_KM) {
                    continue;
                }
                oferta.setDistancia(distanceKm);
            } else {
                oferta.setDistancia(-1.0);
            }

            if (supermercadosSalvos.contains(oferta.getSupermercadoId())) {
                oferta.setSaved(true);
            } else {
                oferta.setSaved(false);
            }

            fetchedOfertas.add(oferta);
        }

        Collections.sort(fetchedOfertas, (o1, o2) -> {
            if (o1.isSaved() && !o2.isSaved()) return -1;
            if (!o1.isSaved() && o2.isSaved()) return 1;

            if (o1.getDistancia() != null && o2.getDistancia() != null && o1.getDistancia() >= 0 && o2.getDistancia() >= 0) {
                return Double.compare(o1.getDistancia(), o2.getDistancia());
            }
            return 0;
        });

        listaOfertas.clear();
        listaOfertas.addAll(fetchedOfertas);
        adapter.setOfertas(listaOfertas);
        if (listaOfertas.isEmpty()) {
            tvListaOfertasMessage.setText("Nenhuma oferta encontrada.");
            tvListaOfertasMessage.setVisibility(View.VISIBLE);
        } else {
            tvListaOfertasMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onOfertaClick(Oferta oferta) {
        if (oferta.getUrlPdf() != null && !oferta.getUrlPdf().isEmpty()) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(oferta.getUrlPdf()));
            startActivity(browserIntent);
        } else {
            Toast.makeText(this, "PDF da oferta não disponível.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveClick(Oferta oferta, int position) {
        if (currentUser == null) {
            Toast.makeText(this, "Faça login para salvar ofertas.", Toast.LENGTH_SHORT).show();
            return;
        }

        String supermercadoId = oferta.getSupermercadoId();
        if (supermercadoId == null || supermercadoId.isEmpty()) {
            Toast.makeText(this, "ID do supermercado não encontrado para salvar.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Tentativa de salvar oferta com supermercadoId nulo/vazio.");
            return;
        }

        if (oferta.isSaved()) {
            usuarioDocRef.update("supermercadosSalvos", FieldValue.arrayRemove(supermercadoId))
                    .addOnSuccessListener(aVoid -> {
                        oferta.setSaved(false);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(this, "Oferta removida dos salvos.", Toast.LENGTH_SHORT).show();
                        carregarOfertas();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erro ao desalvar oferta.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Erro ao desalvar supermercado: " + e.getMessage());
                    });
        } else {
            usuarioDocRef.update("supermercadosSalvos", FieldValue.arrayUnion(supermercadoId))
                    .addOnSuccessListener(aVoid -> {
                        oferta.setSaved(true);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(this, "Oferta salva!", Toast.LENGTH_SHORT).show();
                        carregarOfertas();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erro ao salvar oferta.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Erro ao salvar supermercado: " + e.getMessage());
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ofertasListener != null) {
            ofertasListener.remove();
        }
        if (usuarioPerfilListener != null) {
            usuarioPerfilListener.remove();
        }
        stopLocationUpdates();
    }
}
