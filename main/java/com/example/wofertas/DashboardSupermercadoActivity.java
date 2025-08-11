// app/src/main/java/com/example/wofertas/DashboardSupermercadoActivity.java
package com.example.wofertas;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DashboardSupermercadoActivity extends AppCompatActivity {

    private static final String TAG = "DashboardSupermercado";

    // Visual components
    private EditText etTituloOferta, etDescricaoOferta;
    private TextView tvPdfName;
    private Button btnSelectPdf, btnPublishOffer;
    private ProgressBar progressBarUpload;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private EditText etScheduledDate, etScheduledTime; // Scheduling fields

    // Firebase
    private Uri pdfUri;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Edit mode
    private boolean isEditMode = false;
    private Oferta ofertaParaEditar;

    // Supermarket data (to be filled when publishing/updating)
    private String nomeSupermercadoPerfil;
    private String urlLogoSupermercado;
    private Double latitudeSupermercado;
    private Double longitudeSupermercado;

    // Calendar for scheduling
    private Calendar scheduledCalendar;

    // ActivityResultLauncher to select a PDF file
    private final ActivityResultLauncher<String> pickPdfLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pdfUri = uri;
                    String fileName = getFileNameFromUri(pdfUri);
                    tvPdfName.setText("PDF selecionado: " + fileName);
                    tvPdfName.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "PDF selecionado.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Nenhum arquivo PDF selecionado.", Toast.LENGTH_SHORT).show();
                    tvPdfName.setText("Nenhum PDF selecionado");
                    tvPdfName.setVisibility(View.GONE);
                }
            });

    // ActivityResultLauncher to request storage permission (for Android < Q)
    private final ActivityResultLauncher<String> requestStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Permissão de armazenamento concedida. Abrindo seletor de arquivos...", Toast.LENGTH_SHORT).show();
                    pickPdfLauncher.launch("application/pdf");
                } else {
                    Toast.makeText(this, "Permissão de armazenamento negada. Não é possível selecionar arquivos.", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_supermercado);

        toolbar = findViewById(R.id.toolbar_dashboard);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Publicar Oferta");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Initialize UI components
        etTituloOferta = findViewById(R.id.etTituloOferta);
        etDescricaoOferta = findViewById(R.id.etDescricaoOferta);
        tvPdfName = findViewById(R.id.tvPdfName);
        btnSelectPdf = findViewById(R.id.btnSelectPdf);
        btnPublishOffer = findViewById(R.id.btnPublishOffer);
        progressBarUpload = findViewById(R.id.progressBarUpload);
        bottomNavigationView = findViewById(R.id.bottom_navigation_supermercado);
        etScheduledDate = findViewById(R.id.etScheduledDate);
        etScheduledTime = findViewById(R.id.etScheduledTime);

        // Calendar initialization
        scheduledCalendar = Calendar.getInstance();

        // Firebase initialization
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado. Por favor, faça login.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Configure bottom navigation
        configurarNavegacaoInferior();

        // Check if there's an offer to edit (coming from GerenciarOfertasSupermercadoActivity)
        if (getIntent() != null && getIntent().hasExtra("ofertaParaEditar")) {
            ofertaParaEditar = getIntent().getParcelableExtra("ofertaParaEditar");
            if (ofertaParaEditar != null) {
                isEditMode = true;
                setupEditMode(ofertaParaEditar);
            }
        }

        // Fetch supermarket profile data on Activity start
        buscarDadosPerfilSupermercado();

        // Listener for PDF selection button
        btnSelectPdf.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                pickPdfLauncher.launch("application/pdf");
            }
        });

        // Listeners for date and time selection
        etScheduledDate.setOnClickListener(v -> showDatePickerDialog());
        etScheduledTime.setOnClickListener(v -> showTimePickerDialog());

        // Listener for publish/update offer button
        btnPublishOffer.setOnClickListener(v -> {
            if (isEditMode) {
                updateOffer();
            } else {
                publishOffer();
            }
        });
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
     * Configures the bottom navigation (BottomNavigationView) for the supermarket profile.
     */
    private void configurarNavegacaoInferior() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_publicar_oferta) {
                toolbar.setTitle("Publicar Oferta");
                return true;
            } else if (id == R.id.navigation_dashboard_supermercado) {
                toolbar.setTitle("Minhas Ofertas");
                startActivity(new Intent(this, GerenciarOfertasSupermercadoActivity.class));
                return true;
            } else if (id == R.id.navigation_perfil_supermercado) {
                toolbar.setTitle("Perfil");
                startActivity(new Intent(this, Perfil.class));
                return true;
            }
            return false;
        });
    }



    /**
     * Fetches supermarket profile data (store name, logo URL, lat/long)
     * and stores it in the Activity's global variables.
     * This will be used when saving/updating offers.
     */
    private void buscarDadosPerfilSupermercado() {
        if (currentUser == null) return;

        db.collection("usuarios").document(currentUser.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Usuario usuario = snapshot.toObject(Usuario.class);
                        if (usuario != null && "supermercado".equalsIgnoreCase(usuario.getPerfil())) {
                            nomeSupermercadoPerfil = usuario.getNomeLoja();
                            urlLogoSupermercado = usuario.getUrlLogo();
                            latitudeSupermercado = usuario.getLatitude();
                            longitudeSupermercado = usuario.getLongitude();

                            // Configura subtítulo da Toolbar com o nome do supermercado
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setSubtitle(
                                        nomeSupermercadoPerfil != null ? nomeSupermercadoPerfil : ""
                                );
                            }

                            // Se tiver logo, carrega com Glide, senão usa ícone padrão
                            if (urlLogoSupermercado != null && !urlLogoSupermercado.isEmpty()) {
                                Glide.with(this)
                                        .load(urlLogoSupermercado)
                                        .circleCrop() // Deixa a logo redonda
                                        .into(new CustomTarget<Drawable>() {
                                            @Override
                                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                                toolbar.setNavigationIcon(resource);
                                            }

                                            @Override
                                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                                // Não precisa implementar nada aqui
                                            }
                                        });
                            } else {
                                toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                            }

                            if (nomeSupermercadoPerfil == null || nomeSupermercadoPerfil.isEmpty()) {
                                nomeSupermercadoPerfil = "Nome do Supermercado Desconhecido";
                            }
                            if (urlLogoSupermercado == null) {
                                urlLogoSupermercado = "";
                            }
                        } else {
                            Log.w(TAG, "O usuário logado não é um perfil de supermercado ou o objeto Usuario é nulo.");
                            Toast.makeText(this, "Seu perfil não está configurado como supermercado. Entre em contato com o suporte.", Toast.LENGTH_LONG).show();
                            btnPublishOffer.setEnabled(false);
                            nomeSupermercadoPerfil = "Não Supermercado";
                            urlLogoSupermercado = "";
                            latitudeSupermercado = null;
                            longitudeSupermercado = null;
                            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                        }
                    } else {
                        Log.d(TAG, "Documento do supermercado não encontrado no Firestore para o UID: " + currentUser.getUid());
                        Toast.makeText(this, "Perfil do supermercado não encontrado. Por favor, complete seu cadastro.", Toast.LENGTH_LONG).show();
                        btnPublishOffer.setEnabled(false);
                        nomeSupermercadoPerfil = "Nome do Supermercado Desconhecido";
                        urlLogoSupermercado = "";
                        latitudeSupermercado = null;
                        longitudeSupermercado = null;
                        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar dados do perfil do supermercado: " + e.getMessage());
                    Toast.makeText(this, "Erro ao carregar dados do perfil do supermercado.", Toast.LENGTH_SHORT).show();
                    btnPublishOffer.setEnabled(false);
                    nomeSupermercadoPerfil = "Erro ao Carregar Nome";
                    urlLogoSupermercado = "";
                    latitudeSupermercado = null;
                    longitudeSupermercado = null;
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                });
    }



    /**
     * Configures the UI for edit mode with existing offer data.
     * @param oferta The Offer object to be edited.
     */
    private void setupEditMode(Oferta oferta) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Editar Oferta");
        }
        etTituloOferta.setText(oferta.getDescricao());
        etDescricaoOferta.setText(oferta.getDetalhesOferta());

        if (oferta.getUrlPdf() != null && !oferta.getUrlPdf().isEmpty()) {
            tvPdfName.setText("PDF existente: " + getFileNameFromUrl(oferta.getUrlPdf()));
            tvPdfName.setVisibility(View.VISIBLE);
        } else {
            tvPdfName.setText("Nenhum PDF selecionado");
            tvPdfName.setVisibility(View.GONE);
        }

        // Populate scheduling fields if the offer already has a scheduledAt
        if (oferta.getScheduledAt() != null) {
            scheduledCalendar.setTime(oferta.getScheduledAt().toDate());
            updateScheduledDateEditText();
            updateScheduledTimeEditText();
        } else {
            etScheduledDate.setText("");
            etScheduledTime.setText("");
        }

        btnPublishOffer.setText("Atualizar Oferta");
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "Arquivo desconhecido";
    }

    private String getFileNameFromUrl(String url) {
        try {
            int lastSlashIndex = url.lastIndexOf('/');
            int queryParamIndex = url.indexOf('?');

            if (lastSlashIndex != -1) {
                if (queryParamIndex != -1 && queryParamIndex > lastSlashIndex) {
                    return url.substring(lastSlashIndex + 1, queryParamIndex);
                } else {
                    return url.substring(lastSlashIndex + 1);
                }
            }
            return url;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao analisar o nome do arquivo da URL: " + url, e);
            return "Arquivo";
        }
    }

    /**
     * Displays a DatePickerDialog to select the scheduled date.
     */
    private void showDatePickerDialog() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            scheduledCalendar.set(Calendar.YEAR, year);
            scheduledCalendar.set(Calendar.MONTH, month);
            scheduledCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateScheduledDateEditText();
        }, scheduledCalendar.get(Calendar.YEAR), scheduledCalendar.get(Calendar.MONTH), scheduledCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Displays a TimePickerDialog to select the scheduled time.
     */
    private void showTimePickerDialog() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            scheduledCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            scheduledCalendar.set(Calendar.MINUTE, minute);
            updateScheduledTimeEditText();
        }, scheduledCalendar.get(Calendar.HOUR_OF_DAY), scheduledCalendar.get(Calendar.MINUTE), true).show(); // true for 24h format
    }

    /**
     * Updates the date EditText with the selected date.
     */
    private void updateScheduledDateEditText() {
        String dateFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        etScheduledDate.setText(sdf.format(scheduledCalendar.getTime()));
    }

    /**
     * Updates the time EditText with the selected time.
     */
    private void updateScheduledTimeEditText() {
        String timeFormat = "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, Locale.getDefault());
        etScheduledTime.setText(sdf.format(scheduledCalendar.getTime()));
    }

    /**
     * Publishes a new offer to Firebase Firestore and Storage.
     */
    private void publishOffer() {
        String titulo = etTituloOferta.getText().toString().trim();
        String detalhesOferta = etDescricaoOferta.getText().toString().trim();

        if (titulo.isEmpty() || detalhesOferta.isEmpty()) {
            Toast.makeText(this, "Preencha o título e a descrição detalhada.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pdfUri == null) {
            Toast.makeText(this, "Por favor, selecione o arquivo PDF da oferta.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nomeSupermercadoPerfil == null || nomeSupermercadoPerfil.isEmpty() || latitudeSupermercado == null || longitudeSupermercado == null) {
            Toast.makeText(this, "Aguarde, carregando dados do perfil do supermercado ou complete seu cadastro. Tente novamente.", Toast.LENGTH_LONG).show();
            buscarDadosPerfilSupermercado();
            return;
        }

        // Check if the scheduled date/time is valid (cannot be in the past)
        Timestamp scheduledTimestamp;
        if (!TextUtils.isEmpty(etScheduledDate.getText()) && !TextUtils.isEmpty(etScheduledTime.getText())) {
            scheduledTimestamp = new Timestamp(scheduledCalendar.getTime());
            if (scheduledTimestamp.toDate().before(new Date())) {
                Toast.makeText(this, "A data e hora de agendamento não podem ser no passado.", Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            scheduledTimestamp = null;
        }

        progressBarUpload.setVisibility(View.VISIBLE);
        btnPublishOffer.setEnabled(false);

        String pdfStoragePath = "pdfs/" + currentUser.getUid() + "/" + UUID.randomUUID().toString() + ".pdf";
        Task<Uri> pdfUploadTask = uploadFile(pdfUri, pdfStoragePath);

        pdfUploadTask
                .addOnSuccessListener(pdfDownloadUri -> {
                    String pdfDownloadUrl = pdfDownloadUri.toString();
                    String finalThumbUrl = urlLogoSupermercado; // Use supermarket logo URL as thumbnail

                    saveOfferToFirestore(titulo, detalhesOferta, nomeSupermercadoPerfil, pdfDownloadUrl, finalThumbUrl,
                            currentUser.getUid(), latitudeSupermercado, longitudeSupermercado, scheduledTimestamp);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DashboardSupermercadoActivity.this, "Falha ao enviar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Falha no upload do PDF", e);
                    resetUI();
                });
    }

    /**
     * Updates an existing offer in Firebase Firestore and Storage.
     */
    private void updateOffer() {
        String newTitulo = etTituloOferta.getText().toString().trim();
        String newDetalhesOferta = etDescricaoOferta.getText().toString().trim();

        if (newTitulo.isEmpty() || newDetalhesOferta.isEmpty()) {
            Toast.makeText(this, "Título e descrição detalhada não podem ser vazios.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nomeSupermercadoPerfil == null || nomeSupermercadoPerfil.isEmpty() || latitudeSupermercado == null || longitudeSupermercado == null) {
            Toast.makeText(this, "Aguarde, carregando dados do perfil do supermercado ou complete seu cadastro. Tente novamente.", Toast.LENGTH_LONG).show();
            buscarDadosPerfilSupermercado();
            return;
        }

        // Check if the scheduled date/time is valid (cannot be in the past)
        Timestamp scheduledTimestamp;
        if (!TextUtils.isEmpty(etScheduledDate.getText()) && !TextUtils.isEmpty(etScheduledTime.getText())) {
            scheduledTimestamp = new Timestamp(scheduledCalendar.getTime());
            if (scheduledTimestamp.toDate().before(new Date())) {
                Toast.makeText(this, "A data e hora de agendamento não podem ser no passado.", Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            scheduledTimestamp = null;
        }

        progressBarUpload.setVisibility(View.VISIBLE);
        btnPublishOffer.setEnabled(false);

        Task<Uri> pdfUploadTask;
        String oldPdfUrl = (ofertaParaEditar != null) ? ofertaParaEditar.getUrlPdf() : null;

        if (pdfUri != null) { // If a NEW PDF was selected
            String pdfStoragePath = "pdfs/" + currentUser.getUid() + "/" + UUID.randomUUID().toString() + ".pdf";
            pdfUploadTask = uploadFile(pdfUri, pdfStoragePath);
        } else { // If NO new PDF was selected, use the existing PDF URL
            pdfUploadTask = Tasks.forResult(Uri.parse(oldPdfUrl != null ? oldPdfUrl : ""));
        }

        pdfUploadTask
                .addOnSuccessListener(pdfDownloadUri -> {
                    String finalPdfUrl = pdfDownloadUri.toString();
                    String finalThumbUrl = urlLogoSupermercado; // Use supermarket logo URL as thumbnail

                    // If a new PDF was uploaded and the old PDF existed and was different, delete the old one
                    if (oldPdfUrl != null && !oldPdfUrl.isEmpty() && !oldPdfUrl.equals(finalPdfUrl) && pdfUri != null) {
                        deleteOldFile(oldPdfUrl);
                    }

                    Oferta updatedOferta = new Oferta();
                    updatedOferta.setOfertaId(ofertaParaEditar.getOfertaId());
                    updatedOferta.setDescricao(newTitulo);
                    updatedOferta.setDetalhesOferta(newDetalhesOferta);
                    updatedOferta.setNomeSupermercado(nomeSupermercadoPerfil);
                    updatedOferta.setUrlPdf(finalPdfUrl);
                    updatedOferta.setThumbUrl(finalThumbUrl);
                    updatedOferta.setTimestamp(Timestamp.now()); // Update modification timestamp
                    updatedOferta.setLatitude(latitudeSupermercado);
                    updatedOferta.setLongitude(longitudeSupermercado);
                    updatedOferta.setSupermercadoId(currentUser.getUid());
                    updatedOferta.setScheduledAt(scheduledTimestamp); // Set scheduling

                    // Logic to set the status of the updated offer
                    if (scheduledTimestamp != null && scheduledTimestamp.toDate().after(new Date())) {
                        updatedOferta.setStatus("agendada"); // If scheduled for the future
                    } else {
                        updatedOferta.setStatus("ativa"); // If not scheduled or scheduling has passed, make active
                    }

                    saveUpdatedOfferToFirestore(updatedOferta);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DashboardSupermercadoActivity.this, "Erro no upload ao editar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Falha no upload do PDF ao editar", e);
                    resetUI();
                });
    }

    /**
     * Uploads a file to Firebase Storage.
     * @param fileUri The URI of the local file to be uploaded.
     * @param fullPath The FULL path within Storage where the file will be saved.
     * @return A Task that, when complete, returns the download URL of the file.
     */
    private Task<Uri> uploadFile(Uri fileUri, String fullPath) {
        StorageReference fileRef = storageRef.child(fullPath);
        return fileRef.putFile(fileUri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return fileRef.getDownloadUrl();
        });
    }

    /**
     * Deletes a file from Firebase Storage from its URL.
     * @param oldUrl The download URL of the file to be deleted.
     */
    private void deleteOldFile(String oldUrl) {
        try {
            StorageReference oldFileRef = storage.getReferenceFromUrl(oldUrl);
            oldFileRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Arquivo antigo excluído: " + oldUrl))
                    .addOnFailureListener(e -> Log.w(TAG, "Falha ao excluir arquivo antigo: " + oldUrl + " - " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "URL antiga inválida para exclusão: " + oldUrl + " - " + e.getMessage());
        }
    }

    /**
     * Saves the new offer data to Firebase Firestore.
     * @param titulo The title/short description of the offer.
     * @param detalhesOferta The detailed description of the offer.
     * @param nomeSupermercado The name of the supermarket that published the offer.
     * @param pdfUrl The download URL of the offer PDF.
     * @param thumbUrl The download URL of the offer thumbnail.
     * @param supermercadoId The user ID (supermarket) who published the offer.
     * @param latitude The latitude of the supermarket.
     * @param Longitude The longitude of the supermarket.
     * @param scheduledAt The scheduled date and time for publication (can be null).
     */
    private void saveOfferToFirestore(String titulo, String detalhesOferta, String nomeSupermercado, String pdfUrl, String thumbUrl, String supermercadoId, Double latitude, Double Longitude, Timestamp scheduledAt) {
        Oferta novaOferta = new Oferta();
        novaOferta.setDescricao(titulo);
        novaOferta.setDetalhesOferta(detalhesOferta);
        novaOferta.setNomeSupermercado(nomeSupermercado);
        novaOferta.setUrlPdf(pdfUrl);
        novaOferta.setSupermercadoId(supermercadoId);
        novaOferta.setTimestamp(Timestamp.now()); // Creation timestamp
        novaOferta.setLatitude(latitude);
        novaOferta.setLongitude(Longitude);
        novaOferta.setThumbUrl(thumbUrl);
        novaOferta.setScheduledAt(scheduledAt); // Set scheduling

        // Set initial offer status
        if (scheduledAt != null && scheduledAt.toDate().after(new Date())) {
            novaOferta.setStatus("agendada"); // If scheduled for the future, status is "scheduled"
        } else {
            novaOferta.setStatus("ativa"); // If not scheduled or scheduling has passed, publish immediately as "active"
        }

        db.collection("ofertas").add(novaOferta)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Oferta adicionada com ID: " + documentReference.getId());
                    Toast.makeText(DashboardSupermercadoActivity.this, "Oferta publicada/agendada com sucesso!", Toast.LENGTH_SHORT).show();
                    resetUI();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erro ao adicionar oferta", e);
                    Toast.makeText(DashboardSupermercadoActivity.this, "Erro ao publicar oferta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetUI();
                });
    }

    /**
     * Saves the updated offer data to Firebase Firestore.
     * @param oferta The Offer object with the updated data.
     */
    private void saveUpdatedOfferToFirestore(Oferta oferta) {
        db.collection("ofertas").document(oferta.getOfertaId())
                .set(oferta) // set() overwrites the document
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(DashboardSupermercadoActivity.this, "Oferta atualizada com sucesso!", Toast.LENGTH_SHORT).show();
                    resetUI();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erro ao atualizar oferta", e);
                    Toast.makeText(DashboardSupermercadoActivity.this, "Erro ao atualizar oferta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetUI();
                });
    }

    /**
     * Resets the user interface to its initial state after a publication or edit.
     */
    private void resetUI() {
        etTituloOferta.setText("");
        etDescricaoOferta.setText("");
        tvPdfName.setText("Nenhum PDF selecionado");
        tvPdfName.setVisibility(View.GONE);
        pdfUri = null;
        progressBarUpload.setVisibility(View.GONE);
        btnPublishOffer.setEnabled(true);

        isEditMode = false;
        ofertaParaEditar = null;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Publicar Oferta");
        }
        btnPublishOffer.setText("Publicar Oferta");

        // Reset scheduling fields
        etScheduledDate.setText("");
        etScheduledTime.setText("");
        scheduledCalendar = Calendar.getInstance(); // Reset calendar to current date/time
    }
}
