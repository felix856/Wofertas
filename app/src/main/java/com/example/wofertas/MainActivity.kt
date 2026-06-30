package com.example.wofertas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.wofertas.fcm.NotificationHelper
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var btEntrar: Button
    private lateinit var btCadastrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inicializa canais de notificação (Essencial para Android 8+)
        NotificationHelper.criarCanais(this)

        // 2. Verifica se já está logado
        if (AuthManager.isLoggedIn(this)) {
            // Se logado, garante que o buscador de ofertas em segundo plano está ativo
            agendarBuscaDeOfertas()
            redirecionarPorTipo()
            return
        }

        // 3. Configuração da Interface (Usuário Deslogado)
        btEntrar = findViewById(R.id.btn_entrar)
        btCadastrar = findViewById(R.id.btn_criar_conta)

        btEntrar.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        btCadastrar.setOnClickListener {
            startActivity(Intent(this, Cadastro::class.java))
        }

        // Ajuste de layout para barras do sistema (status bar/navigation bar)
        val mainView = findViewById<android.view.View>(R.id.main)
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
                insets
            }
        }
    }

    /**
     * Configura o WorkManager para buscar novas ofertas a cada 1 hora.
     * Só roda se houver internet para economizar bateria.
     */
    private fun agendarBuscaDeOfertas() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<OfertasWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(OfertasWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            OfertasWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP, // Mantém o agendamento se já existir
            request
        )
    }

    private fun redirecionarPorTipo() {
        val destino = when {
            AuthManager.isMercado(this) -> DashboardSupermercadoActivity::class.java
            AuthManager.isUsuario(this) -> ListaOfertas::class.java
            else -> {
                AuthManager.clearSession(this)
                LoginActivity::class.java
            }
        }

        startActivity(Intent(this, destino).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }
}
