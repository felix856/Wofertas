package com.example.wofertas;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseHelper {

    private static FirebaseAuth auth;

    public static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }

    public static FirebaseUser getUsuarioAtual() {
        return getAuth().getCurrentUser();
    }

    public static String getIdUsuario() {
        FirebaseUser usuario = getUsuarioAtual();
        return usuario != null ? usuario.getUid() : null;
    }

    public static boolean usuarioLogado() {
        return getUsuarioAtual() != null;
    }

    public static void deslogar() {
        getAuth().signOut();
    }
}
