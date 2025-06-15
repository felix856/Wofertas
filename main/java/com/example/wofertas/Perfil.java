package com.example.wofertas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Perfil extends AppCompatActivity {

    private static final String TAG = "PerfilActivity";

    private TextView textViewNomePerfil;
    private TextView textViewEmailPerfil;
    private Button btnTrocarSenha;
    private Button btnSair;

    private FirebaseAuth auth;
    private FirebaseFirestore db; // MUDANÇA IMPORTANTE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // Configura Toolbar com botão de voltar
        Toolbar toolbar = findViewById(R.id.toolbar_perfil);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // Handle clique na seta de voltar
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bind das views
        textViewNomePerfil = findViewById(R.id.textViewNomePerfil);
        textViewEmailPerfil = findViewById(R.id.textViewEmailPerfil);
        btnTrocarSenha = findViewById(R.id.btnTrocarSenha);
        btnSair = findViewById(R.id.btnSair);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Carrega dados do usuário no perfil
        carregarDadosDoUsuario();

        // Configura botão “Trocar Senha”
        btnTrocarSenha.setOnClickListener(v -> {
            String email = auth.getCurrentUser() != null
                    ? auth.getCurrentUser().getEmail()
                    : null;
            if (email != null && !email.isEmpty()) {
                auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(Perfil.this,
                                    "E-mail de redefinição enviado para " + email,
                                    Toast.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Erro ao enviar e-mail de reset: " + e.getMessage());
                            Toast.makeText(Perfil.this,
                                    "Falha ao enviar e-mail: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
            } else {
                Toast.makeText(Perfil.this,
                        "E-mail não disponível para redefinir senha.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Configura botão “Sair”
        btnSair.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(Perfil.this, "Você saiu da conta.", Toast.LENGTH_SHORT).show();
            Intent it = new Intent(Perfil.this, LoginActivity.class);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(it);
            finish();
        });
    }

    /**
     * Lê do Realtime Database (/usuarios/{uid}) para obter nome/nomeLoja.
     */
    private void carregarDadosDoUsuario() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_LONG).show();
            Intent it = new Intent(this, LoginActivity.class);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(it);
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        // Exibe o email diretamente
        String email = auth.getCurrentUser().getEmail();
        if (email != null) {
            textViewEmailPerfil.setText(email);
        }

        // Recupera dados no nó /usuarios/{uid}
        DocumentReference docRef = db.collection("usuarios").document(uid);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String perfil = document.getString("perfil");
                    String nomeExibicao = "Supermercado".equals(perfil)
                            ? document.getString("nomeLoja")
                            : document.getString("nome");
                    textViewNomePerfil.setText(nomeExibicao != null ? nomeExibicao : "Nome não definido");
                } else {
                    Log.d(TAG, "Nenhum documento encontrado");
                    textViewNomePerfil.setText("Perfil não encontrado");
                }
            } else {
                Log.d(TAG, "Falha ao buscar: ", task.getException());
                Toast.makeText(this, "Falha ao carregar perfil.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
