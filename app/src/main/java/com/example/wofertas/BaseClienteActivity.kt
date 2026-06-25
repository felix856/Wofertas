package com.example.wofertas

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Classe base para todas as Activities exclusivas de CLIENTES (USUARIO).
 *
 * Qualquer Activity que herdar esta classe:
 *   - É automaticamente fechada se o usuário não estiver logado
 *   - É automaticamente fechada se o tipo for MERCADO (acesso indevido)
 *   - Não precisa repetir o guard inline
 *
 * Activities que devem herdar:
 *   - ListaOfertas
 *   - CarrinhoActivity
 *   - Mapa (se for exclusivo de clientes)
 */
abstract class BaseClienteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthManager.isLoggedIn(this)) {
            irParaLogin(); return
        }

        if (!AuthManager.isUsuario(this)) {
            Toast.makeText(this, "Área exclusiva para clientes.", Toast.LENGTH_SHORT).show()
            irParaDashboard(); return
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AuthManager.isLoggedIn(this)) irParaLogin()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        ChatbotLauncher.install(this)
    }

    private fun irParaLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun irParaDashboard() {
        startActivity(Intent(this, DashboardSupermercadoActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }
}
