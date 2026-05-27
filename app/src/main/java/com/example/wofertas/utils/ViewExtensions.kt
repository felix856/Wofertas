package com.example.wofertas.utils

import android.util.Base64
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.wofertas.network.ApiClient

/**
 * Exibe a view (VISIBLE).
 */
fun View.show() {
    this.visibility = View.VISIBLE
}

/**
 * Esconde a view mas mantém o espaço (INVISIBLE).
 */
fun View.hide() {
    this.visibility = View.INVISIBLE
}

/**
 * Remove a view do layout (GONE).
 */
fun View.gone() {
    this.visibility = View.GONE
}

/**
 * Alterna entre VISIBLE e GONE.
 */
fun View.toggle() {
    this.visibility = if (this.visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

/**
 * Define o estado habilitado e ajusta a opacidade (alpha).
 */
fun View.setEnableWithAlpha(enabled: Boolean) {
    this.isEnabled = enabled
    this.alpha = if (enabled) 1f else 0.5f
}

/**
 * Carrega uma imagem de forma inteligente:
 * - Se for Base64 (data:image...), decodifica e carrega.
 * - Se for um caminho relativo (/uploads...), completa com a BASE_URL.
 * - Se for uma URL completa, carrega direto.
 * - [skipCache]: Se true, força o download da imagem ignorando o cache local.
 */
fun ImageView.loadImage(path: String?, placeholder: Int = 0, skipCache: Boolean = false) {
    if (path.isNullOrBlank()) {
        if (placeholder != 0) this.setImageResource(placeholder)
        return
    }

    val glideRequest = Glide.with(this.context)
    val builder = when {
        // Caso 1: Imagem em Base64
        path.startsWith("data:image") && path.contains("base64,") -> {
            try {
                val base64String = path.substringAfter("base64,")
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                glideRequest.asBitmap().load(imageBytes)
            } catch (e: Exception) {
                glideRequest.load(placeholder)
            }
        }

        // Caso 2: URL completa
        path.startsWith("http") -> {
            glideRequest.load(path)
        }

        // Caso 3: Caminho relativo do servidor
        else -> {
            val baseUrl = ApiClient.getCurrentBaseUrl().removeSuffix("/")
            val cleanPath = if (path.startsWith("/")) path else "/$path"
            glideRequest.load(baseUrl + cleanPath)
        }
    }

    builder.placeholder(placeholder)
        .error(placeholder)
        .circleCrop()
        
    if (skipCache) {
        builder.diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .signature(ObjectKey(System.currentTimeMillis().toString())) // Garante unicidade
    } else {
        builder.diskCacheStrategy(DiskCacheStrategy.ALL)
    }

    builder.into(this)
}

/**
 * Define o clique com debounce (previne cliques duplos acidentais).
 */
fun View.setOnClickListenerWithDebounce(debounceMs: Long = 500, block: () -> Unit) {
    var lastClickTime = 0L
    this.setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceMs) {
            lastClickTime = currentTime
            block()
        }
    }
}