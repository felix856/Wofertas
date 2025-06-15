package com.example.wofertas;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils; // Import necessário
import android.util.Patterns;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView; // Import necessário
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1001;

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    private EditText edtEmail, edtSenha;
    private Button btContinuar, btVoltar, btCadastro;
    private SignInButton btnGoogle;
    private TextView tvEsqueceuSenha; // ADICIONADO: Referência ao TextView

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseHelper.getAuth();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Referências das Views
        edtEmail = findViewById(R.id.email);
        edtSenha = findViewById(R.id.senha);
        btContinuar = findViewById(R.id.button2);
        btVoltar = findViewById(R.id.button);
        btCadastro = findViewById(R.id.button3);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvEsqueceuSenha = findViewById(R.id.tvEsqueceuSenha); // ADICIONADO: Bind do TextView

        // ADICIONADO: Listener para o clique em "Esqueceu a senha?"
        tvEsqueceuSenha.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                edtEmail.setError("Digite seu e-mail para redefinir a senha.");
                edtEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Digite um e-mail válido.");
                edtEmail.requestFocus();
                return;
            }

            enviarEmailRedefinicaoSenha(email);
        });

        // Configuração dos outros listeners (mantém o que você já tinha)
        configurarListenersDeLogin();
    }

    // ... (onStart e outros métodos permanecem os mesmos) ...
    @Override
    protected void onStart() {
        super.onStart();
        // **MELHORIA ADICIONADA AQUI**
        // Verifica se o usuário já está logado quando a tela inicia
        verificarUsuarioLogado();
    }

    private void verificarUsuarioLogado() {
        if (FirebaseHelper.usuarioLogado()) {
            Toast.makeText(this, "Login automático...", Toast.LENGTH_SHORT).show();
            redirecionarUsuarioPorPerfil(FirebaseHelper.getIdUsuario());
        }
        // Se não estiver logado, não faz nada e a tela de login continua visível.
    }

    // ADICIONADO: Novo método para enviar o e-mail
    private void enviarEmailRedefinicaoSenha(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "E-mail de redefinição enviado para " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Falha ao enviar e-mail: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ... (todo o resto do seu código de LoginActivity.java) ...
    private void configurarListenersDeLogin() {
        // Alternar visibilidade da senha
        edtSenha.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    event.getRawX() >= (edtSenha.getRight() - edtSenha.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                if (edtSenha.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    edtSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    edtSenha.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_off_24, 0); // Troque para um ícone de "olho aberto" se tiver
                } else {
                    edtSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    edtSenha.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_off_24, 0);
                }
                edtSenha.setSelection(edtSenha.getText().length());
                return true;
            }
            return false;
        });

        // Login com email/senha
        btContinuar.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String senha = edtSenha.getText().toString().trim();

            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha email e senha", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, senha)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            redirecionarUsuarioPorPerfil(FirebaseHelper.getIdUsuario());
                        } else {
                            Toast.makeText(this, "Erro ao autenticar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btCadastro.setOnClickListener(v -> startActivity(new Intent(this, Cadastro.class)));
        btVoltar.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        // Login com Google
        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    private void redirecionarUsuarioPorPerfil(String uid) {
        if (uid == null) {
            Toast.makeText(this, "Erro: UID do usuário é nulo.", Toast.LENGTH_SHORT).show();
            return;
        }

        UsuarioHelper.buscarPerfilUsuario(uid, new UsuarioHelper.PerfilCallback() {
            @Override
            public void onPerfilRecebido(String perfil) {
                Intent intent;
                if ("Supermercado".equalsIgnoreCase(perfil)) {
                    intent = new Intent(LoginActivity.this, DashboardSupermercadoActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, ListaOfertas.class);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Fecha a LoginActivity para que o usuário não possa voltar para ela
            }

            @Override
            public void onErro(String erro) {
                // Se não encontrar o perfil, pode ser um novo usuário do Google Sign-In
                // ou um erro. Por segurança, deslogar e pedir para cadastrar.
                Toast.makeText(LoginActivity.this, erro + " Por favor, complete seu cadastro.", Toast.LENGTH_LONG).show();
                // Opcional: Deslogar o usuário se o perfil não for encontrado no banco de dados.
                // FirebaseAuth.getInstance().signOut();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Falha no login com Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        redirecionarUsuarioPorPerfil(FirebaseHelper.getIdUsuario());
                    } else {
                        Toast.makeText(this, "Autenticação com Firebase falhou: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
