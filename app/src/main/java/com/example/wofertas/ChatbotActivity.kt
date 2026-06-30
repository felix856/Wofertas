package com.example.wofertas

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.ApiErrorParser
import com.example.wofertas.network.ChatbotRequest
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var messagesContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var inputMessage: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var progressChat: ProgressBar
    private lateinit var quickOne: MaterialButton
    private lateinit var quickTwo: MaterialButton
    private lateinit var quickThree: MaterialButton

    private val api get() = ApiClient.authService(this)
    private val pageContext: String by lazy {
        intent.getStringExtra(EXTRA_PAGE) ?: "App Android"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthManager.isLoggedIn(this)) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_chatbot)

        toolbar = findViewById(R.id.toolbarChatbot)
        messagesContainer = findViewById(R.id.messagesContainer)
        scrollView = findViewById(R.id.chatScrollView)
        inputMessage = findViewById(R.id.inputChatMessage)
        sendButton = findViewById(R.id.btnSendChat)
        progressChat = findViewById(R.id.progressChat)
        quickOne = findViewById(R.id.btnQuickOne)
        quickTwo = findViewById(R.id.btnQuickTwo)
        quickThree = findViewById(R.id.btnQuickThree)

        configurarToolbar()
        configurarAtalhos()
        configurarEnvio()
        mostrarBoasVindas()
    }

    private fun configurarToolbar() {
        val mercado = AuthManager.isMercado(this)
        toolbar.title = if (mercado) "Assistente do supermercado" else "Assistente do cliente"
        toolbar.subtitle = "Ajuda interativa do Wofertas"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun configurarAtalhos() {
        val perguntas = if (AuthManager.isMercado(this)) {
            listOf(
                getString(R.string.chatbot_quick_market_one),
                getString(R.string.chatbot_quick_market_two),
                getString(R.string.chatbot_quick_market_three)
            )
        } else {
            listOf(
                getString(R.string.chatbot_quick_user_one),
                getString(R.string.chatbot_quick_user_two),
                getString(R.string.chatbot_quick_user_three)
            )
        }

        listOf(quickOne, quickTwo, quickThree).forEachIndexed { index, button ->
            val pergunta = perguntas[index]
            button.text = pergunta
            button.contentDescription = "Perguntar: $pergunta"
            button.setOnClickListener { enviar(pergunta) }
        }
    }

    private fun configurarEnvio() {
        sendButton.setOnClickListener {
            enviar(inputMessage.text?.toString().orEmpty())
        }

        inputMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviar(inputMessage.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    private fun mostrarBoasVindas() {
        val nome = AuthManager.getNome(this)
            ?.takeIf { it.isNotBlank() && !it.equals("Usuario", ignoreCase = true) }
        val textoBase = if (AuthManager.isMercado(this)) {
            getString(R.string.chatbot_welcome_market)
        } else {
            getString(R.string.chatbot_welcome_user)
        }
        val texto = if (nome == null) textoBase else "$textoBase\n\n$nome, como posso ajudar agora?"
        adicionarMensagem(texto, remetenteUsuario = false)
    }

    private fun enviar(textoOriginal: String) {
        val texto = textoOriginal.trim()
        if (texto.isBlank()) {
            Toast.makeText(this, R.string.chatbot_empty_message, Toast.LENGTH_SHORT).show()
            return
        }

        inputMessage.setText("")
        adicionarMensagem(texto, remetenteUsuario = true)
        setCarregando(true)

        lifecycleScope.launch {
            try {
                val response = api.enviarMensagemChatbot(
                    ChatbotRequest(
                        mensagem = texto,
                        pagina = pageContext,
                        contextoTela = if (AuthManager.isMercado(this@ChatbotActivity)) {
                            "mobile-mercado"
                        } else {
                            "mobile-cliente"
                        }
                    )
                )

                when {
                    response.isSuccessful -> {
                        val resposta = response.body()?.resposta.orEmpty()
                        adicionarMensagem(
                            resposta.ifBlank { respostaLocal(texto) },
                            remetenteUsuario = false
                        )
                    }
                    response.code() == 401 -> {
                        AuthManager.clearSession(this@ChatbotActivity)
                        Toast.makeText(this@ChatbotActivity, R.string.session_expired, Toast.LENGTH_LONG).show()
                        goToLogin()
                    }
                    else -> {
                        val erro = ApiErrorParser.parse(response).ifBlank {
                            getString(R.string.chatbot_error_response)
                        }
                        adicionarMensagem(erro, remetenteUsuario = false)
                    }
                }
            } catch (e: Exception) {
                adicionarMensagem(respostaLocal(texto), remetenteUsuario = false)
            } finally {
                setCarregando(false)
            }
        }
    }

    private fun setCarregando(carregando: Boolean) {
        sendButton.isEnabled = !carregando
        inputMessage.isEnabled = !carregando
        progressChat.visibility = if (carregando) View.VISIBLE else View.GONE
        sendButton.text = getString(if (carregando) R.string.chatbot_wait else R.string.chatbot_send)
        if (!carregando) inputMessage.requestFocus()
    }

    private fun adicionarMensagem(texto: String, remetenteUsuario: Boolean) {
        val bubble = TextView(this).apply {
            text = texto
            textSize = 15f
            setLineSpacing(2f, 1.05f)
            setTextColor(
                ContextCompat.getColor(
                    this@ChatbotActivity,
                    if (remetenteUsuario) R.color.white else R.color.text_primary
                )
            )
            setBackgroundResource(if (remetenteUsuario) R.drawable.bg_chat_user else R.drawable.bg_chat_assistant)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = if (remetenteUsuario) {
                "Voce perguntou: $texto"
            } else {
                "Assistente respondeu: $texto"
            }
            if (!remetenteUsuario && messagesContainer.childCount == 0) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (remetenteUsuario) Gravity.END else Gravity.START
            setMargins(dp(16), dp(6), dp(16), dp(6))
            width = (resources.displayMetrics.widthPixels * 0.82f).toInt()
        }

        messagesContainer.addView(bubble, params)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun respostaLocal(pergunta: String): String {
        val msg = pergunta.lowercase()
        return if (AuthManager.isMercado(this)) {
            when {
                "oferta" in msg || "public" in msg -> "Para publicar uma oferta, abra o dashboard do supermercado, preencha nome, validade e imagem da oferta. Para enviar PDF, use o menu Encartes."
                "ranking" in msg -> "O ranking mostra a posicao do mercado conforme o engajamento das ofertas, principalmente curtidas e favoritos."
                "dashboard" in msg || "metric" in msg -> "No dashboard voce acompanha visualizacoes, curtidas, favoritos, carrinhos e recomendacoes para melhorar suas campanhas."
                else -> "Posso ajudar com ofertas, encartes, dashboard, ranking, perfil do mercado e publicacoes."
            }
        } else {
            when {
                "mapa" in msg || "proxima" in msg -> "No mapa, permita a localizacao e toque nos marcadores para ver ofertas de mercados proximos."
                "lista" in msg || "compra" in msg -> "Na lista de compras voce organiza produtos desejados e recebe ajuda para identificar promocoes relacionadas."
                "favorit" in msg || "salv" in msg -> "Toque no botao de favorito para salvar mercados e ofertas que voce quer acompanhar depois."
                else -> "Posso ajudar com mapa, ofertas, favoritos, lista de compras, perfil e notificacoes."
            }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PAGE = "extra_page"
    }
}
