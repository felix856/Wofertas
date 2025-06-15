package com.example.wofertas;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class DashboardSupermercadoActivity extends AppCompatActivity {

    private EditText edtTituloOferta;
    private Button btnSelectPdf, btnPublishOffer;
    private TextView tvPdfName;
    private ProgressBar progressBarUpload;

    private Uri pdfUri;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private StorageReference storageRef;

    // Launcher moderno para permissão de leitura
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    abrirSeletorPdf();
                } else {
                    Toast.makeText(this, "Permissão negada. Impossível selecionar PDF.", Toast.LENGTH_LONG).show();
                }
            });

    // Launcher moderno para selecionar o arquivo PDF
    private final ActivityResultLauncher<String> pdfPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pdfUri = uri;
                    String fileName = getFileName(uri);
                    tvPdfName.setText(fileName);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_supermercado);

        Toolbar toolbar = findViewById(R.id.toolbar_dashboard);
        setSupportActionBar(toolbar);

        edtTituloOferta = findViewById(R.id.edtDiaOferta);
        btnSelectPdf = findViewById(R.id.btnSelectPdf);
        tvPdfName = findViewById(R.id.tvPdfName);
        btnPublishOffer = findViewById(R.id.btnPublishOffer);
        progressBarUpload = findViewById(R.id.progressBarUpload);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("ofertas_pdfs");

        btnSelectPdf.setOnClickListener(v -> verificarPermissaoEabrirSeletor());
        btnPublishOffer.setOnClickListener(v -> validarEpublicarOferta());
    }

    // Método robusto para obter o nome do arquivo a partir de uma Uri
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
            if (result != null && result.contains("/")) {
                result = result.substring(result.lastIndexOf('/') + 1);
            }
        }
        return result != null ? result : "arquivo.pdf";
    }

    private void verificarPermissaoEabrirSeletor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            abrirSeletorPdf();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                abrirSeletorPdf();
            }
        }
    }

    private void abrirSeletorPdf() {
        pdfPickerLauncher.launch("application/pdf");
    }

    private void validarEpublicarOferta() {
        if (pdfUri == null) {
            Toast.makeText(this, "Selecione um PDF primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        String tituloOferta = edtTituloOferta.getText().toString().trim();
        if (tituloOferta.isEmpty()) {
            edtTituloOferta.setError("Digite um título para a oferta.");
            edtTituloOferta.requestFocus();
            return;
        }
        uploadPdfEPublicarOferta(tituloOferta);
    }

    private void uploadPdfEPublicarOferta(String tituloOferta) {
        setLoadingState(true);
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            handleUploadError("Usuário não autenticado. Faça login novamente.");
            return;
        }
        String userId = currentUser.getUid();
        String ofertaId = firestore.collection("ofertas").document().getId();
        StorageReference pdfRef = storageRef.child(ofertaId + ".pdf");

        pdfRef.putFile(pdfUri)
                .addOnSuccessListener(taskSnapshot -> {
                    pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String pdfUrl = uri.toString();
                        buscarDadosUsuarioESalvarOferta(userId, ofertaId, tituloOferta, pdfUrl);
                    }).addOnFailureListener(e -> handleUploadError("Falha ao obter URL do PDF: " + e.getMessage()));
                })
                .addOnFailureListener(e -> handleUploadError("Falha no upload do PDF: " + e.getMessage()));
    }

    private void buscarDadosUsuarioESalvarOferta(String userId, String ofertaId, String tituloOferta, String pdfUrl) {
        firestore.collection("usuarios").document(userId).get()
                .addOnSuccessListener(usuarioSnap -> {
                    if (!usuarioSnap.exists()) {
                        handleUploadError("Perfil do supermercado não encontrado.");
                        return;
                    }
                    String nomeLoja = usuarioSnap.getString("nomeLoja");
                    if (nomeLoja == null) nomeLoja = usuarioSnap.getString("nome");

                    if (!usuarioSnap.contains("latitude") || !usuarioSnap.contains("longitude")) {
                        handleUploadError("Seu perfil de supermercado não possui localização (latitude/longitude). Por favor, contate o suporte.");
                        return;
                    }

                    Map<String, Object> novaOferta = new HashMap<>();
                    novaOferta.put("nome", nomeLoja != null ? nomeLoja : "Nome Indisponível");
                    novaOferta.put("descricao", tituloOferta);
                    novaOferta.put("lojaId", userId);
                    novaOferta.put("urlPdf", pdfUrl);
                    novaOferta.put("timestamp", System.currentTimeMillis());
                    novaOferta.put("latitude", usuarioSnap.getDouble("latitude"));
                    novaOferta.put("longitude", usuarioSnap.getDouble("longitude"));

                    salvarOfertaNoFirestore(ofertaId, novaOferta);
                })
                .addOnFailureListener(e -> handleUploadError("Erro ao obter dados do usuário: " + e.getMessage()));
    }

    private void salvarOfertaNoFirestore(String ofertaId, Map<String, Object> novaOferta) {
        firestore.collection("ofertas").document(ofertaId)
                .set(novaOferta)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Oferta publicada com sucesso!", Toast.LENGTH_LONG).show();
                    resetarCampos();
                    setLoadingState(false);
                })
                .addOnFailureListener(e -> handleUploadError("Erro ao salvar oferta: " + e.getMessage()));
    }

    private void setLoadingState(boolean isLoading) {
        progressBarUpload.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnPublishOffer.setEnabled(!isLoading);
        btnSelectPdf.setEnabled(!isLoading);
    }

    private void resetarCampos() {
        edtTituloOferta.setText("");
        tvPdfName.setText("Nenhum PDF selecionado");
        pdfUri = null;
    }

    private void handleUploadError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e("PublishOffer", message);
        setLoadingState(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_perfil_dashboard) {
            Intent intent = new Intent(this, Perfil.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
