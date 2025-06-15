package com.example.wofertas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private Button btEntrar, btCadastrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Chama explicitamente a versão de AppCompatActivity
        super.setContentView(R.layout.activity_main);

        // Inicializa o Firebase
        FirebaseApp.initializeApp(this);
        auth = FirebaseHelper.getAuth();
        user = FirebaseHelper.getUsuarioAtual();
        btEntrar    = findViewById(R.id.button);
        btCadastrar  = findViewById(R.id.button2);

        if (user != null) {
            Log.d("Firebase", "Usuário logado: " + user.getEmail());
        } else {
            Log.d("Firebase", "Nenhum usuário logado.");
        }

        // Ajusta paddings para edge-to-edge (API 30+)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Cadastro
        btEntrar.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class))
        );

        // Voltar
        btCadastrar.setOnClickListener(v -> startActivity(new Intent(MainActivity.this,Cadastro.class))
        );
    }
}