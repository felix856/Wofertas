package com.example.wofertas;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UsuarioHelper {

    public interface PerfilCallback {
        void onPerfilRecebido(String perfil);
        void onErro(String erro);
    }

    public static void buscarPerfilUsuario(String uid, PerfilCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onErro("UID do usuário é inválido.");
            return;
        }

        // MUDANÇA IMPORTANTE: Busca o documento no Firestore
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String perfil = documentSnapshot.getString("perfil");
                        if (perfil != null) {
                            callback.onPerfilRecebido(perfil);
                        } else {
                            callback.onErro("Campo 'perfil' não encontrado no documento.");
                        }
                    } else {
                        callback.onErro("Perfil de usuário não encontrado no banco de dados.");
                    }
                })
                .addOnFailureListener(e -> callback.onErro("Erro ao acessar banco: " + e.getMessage()));
    }
}
