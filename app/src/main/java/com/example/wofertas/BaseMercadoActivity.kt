package com.example.wofertas

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Classe base para todas as Activities exclusivas de SUPERMERCADOS (MERCADO).
 *
 * Qualquer Activity que herdar esta classe:
 *   - É automaticamente fechada se o usuário não estiver logado
 *   - É automaticamente fechada se o tipo for USUARIO (acesso indevido)
 *   - Não precisa repetir o guard inline
 *
 * Activities que devem herdar:
 *   - DashboardSupermercadoActivity
 *   - GerenciarOfertasSupermercadoActivity
 *   - PublicarOfertaActivity
 */
abstract class BaseMercadoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthManager.isLoggedIn(this)) {
            irParaLogin(); return
        }

        if (!AuthManager.isMercado(this)) {
            Toast.makeText(this, "Área exclusiva para supermercados.", Toast.LENGTH_SHORT).show()
            irParaListaOfertas(); return
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AuthManager.isLoggedIn(this)) irParaLogin()
    }

    private fun irParaLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun irParaListaOfertas() {
        startActivity(Intent(this, ListaOfertas::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }
}
