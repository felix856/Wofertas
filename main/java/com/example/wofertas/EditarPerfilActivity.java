// app/src/main/java/com/example/wofertas/EditarPerfilActivity.java
package com.example.wofertas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView; // Certifique-se de ter esta dependência no seu build.gradle

public class EditarPerfilActivity extends AppCompatActivity {

    private static final String TAG = "EditarPerfilActivity";

    // Componentes da UI
    private CircleImageView imageViewProfile;
    private Button btnSelectImage, btnSaveProfile;
    private EditText etNome, etEmail, etLatitude, etLongitude;
    private TextView tvLatitudeLabel, tvLongitudeLabel;
    private ProgressBar progressBarEditProfile;
    private Toolbar toolbar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private Uri selectedImageUri; // URI da imagem selecionada
    private String currentImageUrl; // URL da imagem atual do perfil no Firestore
    private String userProfileType; // Tipo de perfil (cliente ou supermercado)

    // ActivityResultLauncher para selecionar imagem
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imageViewProfile.setImageURI(selectedImageUri); // Exibe a imagem selecionada
                    Toast.makeText(this, "Imagem selecionada.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Nenhuma imagem selecionada.", Toast.LENGTH_SHORT).show();
                }
            });

    // ActivityResultLauncher para solicitar permissão de armazenamento (para Android < Q)
    private final ActivityResultLauncher<String> requestStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Permissão de armazenamento concedida. Abrindo seletor de imagens...", Toast.LENGTH_SHORT).show();
                    pickImageLauncher.launch("image/*"); // Abre o seletor de imagens
                } else {
                    Toast.makeText(this, "Permissão de armazenamento negada. Não é possível selecionar imagens.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        // Inicialização da Toolbar
        toolbar = findViewById(R.id.toolbar_editar_perfil);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Editar Perfil");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Inicialização dos componentes da UI
        imageViewProfile = findViewById(R.id.imageViewProfile);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        etNome = findViewById(R.id.etNome);
        etEmail = findViewById(R.id.etEmail);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        tvLatitudeLabel = findViewById(R.id.tvLatitudeLabel);
        tvLongitudeLabel = findViewById(R.id.tvLongitudeLabel);
        progressBarEditProfile = findViewById(R.id.progressBarEditProfile);

        // Inicialização Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Carrega os dados do perfil atual
        loadUserProfile();

        // Listener para selecionar imagem
        btnSelectImage.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                pickImageLauncher.launch("image/*"); // Abre o seletor de imagens
            }
        });

        // Listener para salvar o perfil
        btnSaveProfile.setOnClickListener(v -> saveProfile());
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
     * Carrega os dados do perfil do usuário do Firestore e preenche a UI.
     * Ajusta a visibilidade dos campos com base no tipo de perfil.
     */
    private void loadUserProfile() {
        progressBarEditProfile.setVisibility(View.VISIBLE);
        db.collection("usuarios").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBarEditProfile.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        Usuario usuario = documentSnapshot.toObject(Usuario.class);
                        if (usuario != null) {
                            etEmail.setText(currentUser.getEmail()); // Email do Firebase Auth
                            userProfileType = usuario.getPerfil();

                            // Configura a UI com base no tipo de perfil
                            if ("supermercado".equalsIgnoreCase(userProfileType)) {
                                etNome.setText(usuario.getNomeLoja());
                                etNome.setHint("Nome da Loja");

                                // Campos de localização visíveis para supermercado
                                tvLatitudeLabel.setVisibility(View.VISIBLE);
                                etLatitude.setVisibility(View.VISIBLE);
                                tvLongitudeLabel.setVisibility(View.VISIBLE);
                                etLongitude.setVisibility(View.VISIBLE);

                                if (usuario.getLatitude() != null) {
                                    etLatitude.setText(String.valueOf(usuario.getLatitude()));
                                }
                                if (usuario.getLongitude() != null) {
                                    etLongitude.setText(String.valueOf(usuario.getLongitude()));
                                }
                                currentImageUrl = usuario.getUrlLogo(); // URL da logo do supermercado
                                if (!TextUtils.isEmpty(currentImageUrl)) {
                                    Glide.with(this).load(currentImageUrl).placeholder(R.drawable.logo_supermercado_placeholder).error(R.drawable.logo_supermercado_placeholder).into(imageViewProfile);
                                } else {
                                    imageViewProfile.setImageResource(R.drawable.logo_supermercado_placeholder);
                                }

                            } else { // Assume que é "cliente" ou qualquer outro tipo
                                etNome.setText(usuario.getNome());
                                etNome.setHint("Seu Nome");

                                // Campos de localização ocultos para cliente
                                tvLatitudeLabel.setVisibility(View.GONE);
                                etLatitude.setVisibility(View.GONE);
                                tvLongitudeLabel.setVisibility(View.GONE);
                                etLongitude.setVisibility(View.GONE);

                                currentImageUrl = usuario.getUrlFoto(); // URL da foto do cliente
                                if (!TextUtils.isEmpty(currentImageUrl)) {
                                    Glide.with(this).load(currentImageUrl).placeholder(R.drawable.perfil).error(R.drawable.perfil).into(imageViewProfile);
                                } else {
                                    imageViewProfile.setImageResource(R.drawable.perfil);
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Documento de perfil não encontrado para o UID: " + currentUser.getUid());
                        Toast.makeText(this, "Perfil não encontrado. Por favor, complete seus dados.", Toast.LENGTH_LONG).show();
                        etEmail.setText(currentUser.getEmail());
                        userProfileType = "cliente"; // Define como cliente padrão se não encontrar perfil
                        imageViewProfile.setImageResource(R.drawable.perfil); // Imagem padrão para cliente
                        // Oculta campos de localização por padrão para novos perfis sem tipo definido
                        tvLatitudeLabel.setVisibility(View.GONE);
                        etLatitude.setVisibility(View.GONE);
                        tvLongitudeLabel.setVisibility(View.GONE);
                        etLongitude.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBarEditProfile.setVisibility(View.GONE);
                    Log.e(TAG, "Erro ao carregar dados do perfil: " + e.getMessage());
                    Toast.makeText(this, "Erro ao carregar dados do perfil.", Toast.LENGTH_SHORT).show();
                    // Oculta campos de localização em caso de erro
                    tvLatitudeLabel.setVisibility(View.GONE);
                    etLatitude.setVisibility(View.GONE);
                    tvLongitudeLabel.setVisibility(View.GONE);
                    etLongitude.setVisibility(View.GONE);
                });
    }

    /**
     * Salva as alterações no perfil do usuário.
     * Inclui upload de imagem se uma nova imagem foi selecionada.
     */
    private void saveProfile() {
        progressBarEditProfile.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        String nome = etNome.getText().toString().trim();
        Double latitude = null;
        Double longitude = null;

        if (TextUtils.isEmpty(nome)) {
            Toast.makeText(this, "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show();
            progressBarEditProfile.setVisibility(View.GONE);
            btnSaveProfile.setEnabled(true);
            return;
        }

        if ("supermercado".equalsIgnoreCase(userProfileType)) {
            try {
                if (!TextUtils.isEmpty(etLatitude.getText().toString())) {
                    latitude = Double.parseDouble(etLatitude.getText().toString());
                }
                if (!TextUtils.isEmpty(etLongitude.getText().toString())) {
                    longitude = Double.parseDouble(etLongitude.getText().toString());
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Latitude e Longitude devem ser números válidos.", Toast.LENGTH_LONG).show();
                progressBarEditProfile.setVisibility(View.GONE);
                btnSaveProfile.setEnabled(true);
                return;
            }
        }

        // Se uma nova imagem foi selecionada, faz o upload primeiro
        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(nome, latitude, longitude);
        } else {
            // Se nenhuma nova imagem foi selecionada, apenas salva os dados de texto
            // Usa a URL da imagem atual (currentImageUrl)
            saveProfileDataToFirestore(nome, currentImageUrl, latitude, longitude);
        }
    }

    /**
     * Faz o upload da imagem selecionada para o Firebase Storage.
     * @param nome O nome do usuário/loja.
     * @param latitude A latitude (para supermercado).
     * @param longitude A longitude (para supermercado).
     */
    private void uploadImageAndSaveProfile(String nome, Double latitude, Double longitude) {
        String imagePath;
        // Caminho no Storage: users/{UID}/profile_images/{nome_do_arquivo_unico}.jpg
        // Este caminho é unificado para cliente e supermercado, pois a regra do Storage é a mesma.
        imagePath = "users/" + currentUser.getUid() + "/profile_images/" + UUID.randomUUID().toString() + ".jpg";

        StorageReference imageRef = storageRef.child(imagePath);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    Log.d(TAG, "Imagem de perfil uploaded: " + imageUrl);

                    // Se havia uma imagem antiga e uma nova foi enviada, exclua a antiga
                    // Isso evita acumular imagens antigas no Storage
                    if (!TextUtils.isEmpty(currentImageUrl) && !currentImageUrl.equals(imageUrl)) {
                        deleteOldImage(currentImageUrl);
                    }

                    saveProfileDataToFirestore(nome, imageUrl, latitude, longitude);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha no upload da imagem: " + e.getMessage(), e);
                    Toast.makeText(EditarPerfilActivity.this, "Falha no upload da imagem: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBarEditProfile.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                });
    }

    /**
     * Exclui a imagem antiga do Firebase Storage.
     * @param oldImageUrl A URL da imagem antiga a ser excluída.
     */
    private void deleteOldImage(String oldImageUrl) {
        try {
            StorageReference oldImageRef = storage.getReferenceFromUrl(oldImageUrl);
            oldImageRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Imagem antiga de perfil excluída: " + oldImageUrl))
                    .addOnFailureListener(e -> Log.w(TAG, "Falha ao excluir imagem antiga de perfil: " + oldImageUrl + " - " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "URL antiga inválida para exclusão da imagem de perfil: " + oldImageUrl + " - " + e.getMessage());
        }
    }

    /**
     * Salva os dados do perfil (incluindo URL da imagem) no Firestore.
     * Os campos salvos dependem do tipo de perfil.
     * @param nome O nome do usuário/loja.
     * @param imageUrl A URL da imagem de perfil.
     * @param latitude A latitude (para supermercado, pode ser null para cliente).
     * @param longitude A longitude (para supermercado, pode ser null para cliente).
     */
    private void saveProfileDataToFirestore(String nome, String imageUrl, Double latitude, Double longitude) {
        Map<String, Object> updates = new HashMap<>();

        if ("supermercado".equalsIgnoreCase(userProfileType)) {
            updates.put("nomeLoja", nome); // Nome da loja para supermercado
            updates.put("urlLogo", imageUrl); // URL da logo para supermercado
            updates.put("latitude", latitude);
            updates.put("longitude", longitude);
        } else { // Cliente (ou qualquer outro perfil que não seja "supermercado")
            updates.put("nome", nome); // Nome do cliente
            updates.put("urlFoto", imageUrl); // URL da foto para cliente
            // Campos de latitude e longitude NÃO são adicionados para clientes
        }

        db.collection("usuarios").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditarPerfilActivity.this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    progressBarEditProfile.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    finish(); // Retorna para a tela de Perfil
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao atualizar perfil no Firestore: " + e.getMessage(), e);
                    Toast.makeText(EditarPerfilActivity.this, "Erro ao salvar perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBarEditProfile.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                });
    }
}
