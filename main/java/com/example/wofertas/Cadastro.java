package com.example.wofertas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Cadastro extends AppCompatActivity {
    private static final int REQ_PERM_LOC = 1001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private RadioGroup rgPerfil;
    private RadioButton rbCliente, rbSupermercado;
    private EditText edtNome, edtCNPJ, edtTelefone, edtEndereco, edtEmail, edtSenha, edtConfirmaSenha;
    private Button btnContinuar, btnVoltar;

    private Executor executor = Executors.newSingleThreadExecutor();
    private Geocoder geocoder;
    private FusedLocationProviderClient fusedLocationClient;
    private String uidSalvo;
    private boolean precisaCapturarLocalizacaoCliente = false; // Renomeado para clareza

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        auth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();
        rgPerfil = findViewById(R.id.rgPerfil);
        rbCliente = findViewById(R.id.rbCliente);
        rbSupermercado = findViewById(R.id.rbSupermercado);
        edtNome = findViewById(R.id.edtNome);
        edtCNPJ = findViewById(R.id.edtCNPJ); // Certifique-se que o ID no XML é edtCNPJ
        edtTelefone = findViewById(R.id.edtTelefone);
        edtEndereco = findViewById(R.id.edtEndereco);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        edtConfirmaSenha = findViewById(R.id.edtConfirmaSenha);
        btnContinuar = findViewById(R.id.btnContinuar);
        btnVoltar = findViewById(R.id.btnVoltar);

        geocoder = new Geocoder(this, Locale.getDefault());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configuração inicial da UI baseada no RadioButton default (Cliente)
        atualizarCamposPorPerfil();

        rgPerfil.setOnCheckedChangeListener((group, checkedId) -> {
            atualizarCamposPorPerfil();
        });

        btnContinuar.setOnClickListener(v -> {
            String perfil = (rbSupermercado.isChecked() ? "Supermercado" : "Cliente");
            String nome = edtNome.getText().toString().trim();
            String cnpj = edtCNPJ.getText().toString().trim(); // Coleta o CNPJ
            String tel = edtTelefone.getText().toString().trim();
            String enderecoTexto = edtEndereco.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String senha = edtSenha.getText().toString().trim();
            String conf = edtConfirmaSenha.getText().toString().trim();

            // Validações
            if (nome.isEmpty() || tel.isEmpty() || email.isEmpty() || senha.isEmpty() || conf.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show();
                return;
            }

            if (perfil.equals("Supermercado")) {
                if (cnpj.isEmpty()) {
                    edtCNPJ.setError("CNPJ é obrigatório para supermercado");
                    edtCNPJ.requestFocus();
                    return;
                }
                if (cnpj.length() != 14) { // Validação simples de tamanho
                    edtCNPJ.setError("CNPJ deve ter 14 dígitos.");
                    edtCNPJ.requestFocus();
                    return;
                }
                if (enderecoTexto.isEmpty()) { // Endereço ainda é necessário para geocodificação
                    edtEndereco.setError("Endereço é obrigatório para supermercado (para geolocalização)");
                    edtEndereco.requestFocus();
                    return;
                }
            }
            // Para cliente, o campo edtEndereco não é validado aqui pois é opcional ou pego por GPS

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("E-mail inválido");
                edtEmail.requestFocus();
                return;
            }
            if (!senha.equals(conf)) {
                edtConfirmaSenha.setError("As senhas não coincidem");
                edtConfirmaSenha.requestFocus();
                return;
            }
            if (senha.length() < 6) {
                edtSenha.setError("A senha deve ter no mínimo 6 caracteres");
                edtSenha.requestFocus();
                return;
            }

            // Cria usuário no FirebaseAuth
            auth.createUserWithEmailAndPassword(email, senha)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            uidSalvo = auth.getCurrentUser().getUid();
                            Map<String, Object> usuarioMap = new HashMap<>();
                            usuarioMap.put("perfil", perfil);
                            usuarioMap.put("telefone", tel);
                            usuarioMap.put("email", email); // Email já é parte do Firebase Auth user

                            if (perfil.equals("Supermercado")) {
                                usuarioMap.put("nomeLoja", nome); // Ou apenas "nome" se preferir
                                usuarioMap.put("cnpj", cnpj);     // Salva o CNPJ
                                usuarioMap.put("endereco", enderecoTexto); // Endereço textual para geocoding
                                // lat/lng serão adicionados após geocoding
                            } else { // Cliente
                                usuarioMap.put("nome", nome);
                                // CPF foi removido
                                // lat/lng do cliente serão adicionados após permissão e captura
                            }

                            db.collection("usuarios").document(uidSalvo)
                                    .set(usuarioMap)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Cadastro realizado!", Toast.LENGTH_SHORT).show();
                                        if (perfil.equals("Supermercado")) {
                                            executor.execute(() -> geocodificarEnderecoDoMercado(uidSalvo, enderecoTexto));
                                        } else {
                                            precisaCapturarLocalizacaoCliente = true;
                                            solicitarPermissaoLocalizacaoCliente();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Erro ao salvar perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();

                                        if (auth.getCurrentUser() != null) { // Evita crash se o usuário já foi deletado
                                            auth.getCurrentUser().delete();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Falha no cadastro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnVoltar.setOnClickListener(v -> finish());
    }

    private void atualizarCamposPorPerfil() {
        if (rbSupermercado.isChecked()) {
            edtNome.setHint("Nome da Loja");
            edtCNPJ.setVisibility(View.VISIBLE);
            edtEndereco.setVisibility(View.VISIBLE); // Endereço é usado para geocodificar o supermercado
            edtEndereco.setHint("Endereço do Supermercado (para localização)");
        } else { // Cliente
            edtNome.setHint("Nome Completo");
            edtCNPJ.setVisibility(View.GONE);
            edtCNPJ.setText(""); // Limpa o campo se estava preenchido
            edtEndereco.setVisibility(View.GONE); // Cliente usa localização GPS, não endereço digitado
            edtEndereco.setText("");
        }
    }

    private void geocodificarEnderecoDoMercado(String uid, String enderecoTexto) {
        try {
            List<Address> resultados = geocoder.getFromLocationName(enderecoTexto, 1);
            if (resultados != null && !resultados.isEmpty()) {
                Address addr = resultados.get(0);
                Map<String, Object> latlng = new HashMap<>();
                latlng.put("latitude", addr.getLatitude());
                latlng.put("longitude", addr.getLongitude());

                db.collection("usuarios").document(uid).update("latitude",addr.getLatitude(), "longitude", addr.getLongitude())
                        .addOnSuccessListener(aVoid -> irParaDashboardSupermercado())
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> Toast.makeText(Cadastro.this, "Cadastro concluído, mas falha ao salvar coordenadas.", Toast.LENGTH_LONG).show());
                            irParaDashboardSupermercado();
                        });
            } else {
                runOnUiThread(() -> Toast.makeText(Cadastro.this,"Falha ao salvar coordenadas.", Toast.LENGTH_LONG).show());
                irParaDashboardSupermercado();
            }
        } catch (IOException e) {
            runOnUiThread(() -> {
                Toast.makeText(Cadastro.this, "Erro ao geocodificar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                irParaDashboardSupermercado();
            });
        }
    }

    private void irParaDashboardSupermercado() {
        runOnUiThread(() -> {
            startActivity(new Intent(Cadastro.this, DashboardSupermercadoActivity.class));
            finishAffinity(); // Fecha todas as activities anteriores da pilha
        });
    }

    private void solicitarPermissaoLocalizacaoCliente() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERM_LOC);
        } else {
            obterLocalizacaoCliente();
        }
    }

    private void obterLocalizacaoCliente() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Se chegou aqui sem permissão, algo está errado, mas o onRequestPermissionsResult deve tratar.
            // Ou o usuário negou e não deveríamos tentar de novo sem um contexto melhor.
            irParaListaOfertas(); // Vai para a lista mesmo sem localização
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        salvarLocalizacaoClienteNoFirebase(location);
                    } else {
                        Toast.makeText(this, "Não foi possível obter localização. Verifique o GPS.", Toast.LENGTH_SHORT).show();
                        irParaListaOfertas();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao obter localização: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    irParaListaOfertas();
                });
    }

    private void salvarLocalizacaoClienteNoFirebase(Location loc) {
        // MUDANÇA IMPORTANTE: Atualiza o documento no Firestore
        db.collection("usuarios").document(uidSalvo)
                .update("latitude", loc.getLatitude(), "longitude", loc.getLongitude())
                .addOnSuccessListener(aVoid -> irParaListaOfertas())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao salvar localização.", Toast.LENGTH_LONG).show();
                    irParaListaOfertas();
                });
    }

    private void irParaListaOfertas() {
        startActivity(new Intent(this, ListaOfertas.class));
        finishAffinity(); // Fecha todas as activities anteriores da pilha
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM_LOC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (precisaCapturarLocalizacaoCliente) { // Verifica se era para o cliente
                    obterLocalizacaoCliente();
                }
                // Se fosse para supermercado, a lógica de recadastro deveria ser chamada aqui
                // Mas o fluxo atual já tenta geocodificar o endereço do supermercado.
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_LONG).show();
                if (precisaCapturarLocalizacaoCliente) {
                    irParaListaOfertas(); // Cliente vai para lista mesmo sem localização
                }
                // Para supermercado, o cadastro pode prosseguir sem lat/lng se a geocodificação falhar ou a permissão for negada
            }
        }
    }
}
