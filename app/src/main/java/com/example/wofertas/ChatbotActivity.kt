package com.example.wofertas

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.ChatbotRequest
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var messagesContainer: LinearLayout
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var quickOne: Button
    private lateinit var quickTwo: Button
    private lateinit var quickThree: Button

    private val pageContext: String by lazy {
        intent.getStringExtra(EXTRA_PAGE) ?: "App Android"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        if (!AuthManager.isLoggedIn(this)) {
            goToLogin()
            return
        }

        findViewById<Toolbar>(R.id.toolbarChatbot).apply {
            setNavigationOnClickListener { finish() }
        }

        scrollView = findViewById(R.id.chatbotScroll)
        messagesContainer = findViewById(R.id.chatbotMessages)
        input = findViewById(R.id.chatbotInput)
        sendButton = findViewById(R.id.chatbotSend)
        progressBar = findViewById(R.id.chatbotProgress)
        quickOne = findViewById(R.id.chatbotQuickOne)
        quickTwo = findViewById(R.id.chatbotQuickTwo)
        quickThree = findViewById(R.id.chatbotQuickThree)

        configureQuickQuestions()
        sendButton.setOnClickListener { sendCurrentMessage() }

        showWelcome()
    }

    private fun configureQuickQuestions() {
        val questions = if (AuthManager.isMercado(this)) {
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
            button.text = questions[index]
            button.setOnClickListener {
                input.setText(questions[index])
                input.setSelection(input.text.length)
                sendCurrentMessage()
            }
        }
    }

    private fun showWelcome() {
        val name = AuthManager.getNome(this)?.takeIf { it.isNotBlank() && it != "Usuario" }
        val typeHint = if (AuthManager.isMercado(this)) {
            getString(R.string.chatbot_welcome_market)
        } else {
            getString(R.string.chatbot_welcome_user)
        }
        addMessage(
            if (name == null) typeHint else "$typeHint\n\n$name, como posso ajudar agora?",
            fromUser = false
        )
    }

    private fun sendCurrentMessage() {
        val message = input.text.toString().trim()
        if (message.isBlank()) {
            Toast.makeText(this, R.string.chatbot_empty_message, Toast.LENGTH_SHORT).show()
            return
        }

        input.setText("")
        addMessage(message, fromUser = true)
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.authService(this@ChatbotActivity).enviarMensagemChatbot(
                    ChatbotRequest(
                        mensagem = message,
                        pagina = pageContext,
                        contextoTela = currentContext()
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    addMessage(
                        body?.resposta?.takeIf { it.isNotBlank() }
                            ?: getString(R.string.chatbot_empty_response),
                        fromUser = false
                    )
                } else if (response.code() == 401) {
                    AuthManager.clearSession(this@ChatbotActivity)
                    Toast.makeText(this@ChatbotActivity, R.string.session_expired, Toast.LENGTH_LONG).show()
                    goToLogin()
                } else {
                    addMessage(getString(R.string.chatbot_error_response), fromUser = false)
                }
            } catch (e: Exception) {
                addMessage(getString(R.string.chatbot_network_error), fromUser = false)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun currentContext(): String {
        val tipo = AuthManager.getTipo(this) ?: "ANONIMO"
        return "tipo=$tipo; tela=$pageContext"
    }

    private fun addMessage(text: String, fromUser: Boolean) {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 15f
            setLineSpacing(2f, 1f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            maxWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
            setTextColor(
                ContextCompat.getColor(
                    this@ChatbotActivity,
                    if (fromUser) R.color.white else R.color.text_primary
                )
            )
            setBackgroundResource(if (fromUser) R.drawable.bg_chat_user else R.drawable.bg_chat_assistant)
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (fromUser) Gravity.END else Gravity.START
            setMargins(dp(12), dp(6), dp(12), dp(6))
        }

        messagesContainer.addView(bubble, params)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        sendButton.isEnabled = !loading
        input.isEnabled = !loading
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PAGE = "extra_page"
    }
}
