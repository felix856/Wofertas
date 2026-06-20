package com.example.wofertas.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.ApiErrorParser
import com.example.wofertas.network.OfertaRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class PublicarOfertaViewModel(private val context: Context) : ViewModel() {

    private val api get() = ApiClient.authService(context)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _evento = MutableSharedFlow<PublicarEvento>()
    val evento: SharedFlow<PublicarEvento> = _evento.asSharedFlow()

    private val _imagemBase64 = MutableStateFlow<String?>(null)
    val imagemBase64: StateFlow<String?> = _imagemBase64.asStateFlow()

    /**
     * CORREÇÃO: Comprime a imagem para evitar Payload Too Large (Erro 400).
     */
    fun processarImagem(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val base64 = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmapOriginal = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmapOriginal != null) {
                        val outputStream = ByteArrayOutputStream()
                        // Comprime para 40% da qualidade original para reduzir o tamanho do JSON
                        bitmapOriginal.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
                        val bytes = outputStream.toByteArray()
                        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                    } else null
                }
                _imagemBase64.value = base64
            } catch (e: Exception) {
                _evento.emit(PublicarEvento.Erro("Erro ao processar imagem: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * CORREÇÃO: Formata a data para ISO (yyyy-MM-dd) para evitar erro 400 no Backend.
     */
    fun criarOferta(nome: String, status: String, data: String) {
        val dataFormatada = formatarDataParaIso(data)

        if (!validar(nome, dataFormatada)) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val body = OfertaRequest(
                    nome         = nome,
                    status       = status.ifBlank { "ATIVO" },
                    data         = dataFormatada,
                    imagemOferta = _imagemBase64.value
                )

                val resp = api.criarOferta(body)
                if (resp.isSuccessful) {
                    _evento.emit(PublicarEvento.Sucesso("Oferta publicada com sucesso!"))
                } else {
                    val erroMsg = ApiErrorParser.parse(resp)
                    Log.e("API_ERROR", "Erro 400/500: $erroMsg")
                    _evento.emit(PublicarEvento.Erro(erroMsg))
                }
            } catch (e: Exception) {
                _evento.emit(PublicarEvento.Erro(ApiErrorParser.fromException(e)))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun atualizarOferta(id: String, nome: String, status: String, data: String) {
        val dataFormatada = formatarDataParaIso(data)
        if (!validar(nome, dataFormatada)) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val body = OfertaRequest(
                    nome         = nome,
                    status       = status.ifBlank { "ATIVO" },
                    data         = dataFormatada,
                    imagemOferta = _imagemBase64.value
                )
                val resp = api.atualizarOferta(id, body)
                if (resp.isSuccessful) {
                    _evento.emit(PublicarEvento.Sucesso("Oferta atualizada com sucesso!"))
                } else {
                    _evento.emit(PublicarEvento.Erro(ApiErrorParser.parse(resp)))
                }
            } catch (e: Exception) {
                _evento.emit(PublicarEvento.Erro(ApiErrorParser.fromException(e)))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Converte "dd/MM/yyyy" para "yyyy-MM-dd".
     */
    private fun formatarDataParaIso(data: String): String {
        return try {
            if (data.contains("/")) {
                val partes = data.split("/")
                if (partes.size == 3) {
                    // Assume formato dd/MM/yyyy -> retorna yyyy-MM-dd
                    "${partes[2]}-${partes[1]}-${partes[0]}"
                } else data
            } else data
        } catch (e: Exception) {
            data
        }
    }

    private fun validar(nome: String, data: String): Boolean {
        if (nome.isBlank()) {
            viewModelScope.launch { _evento.emit(PublicarEvento.ValidacaoErro("nome", "Informe o nome")) }
            return false
        }
        if (data.isBlank()) {
            viewModelScope.launch { _evento.emit(PublicarEvento.ValidacaoErro("data", "Informe a data")) }
            return false
        }
        return true
    }

    fun limparImagem() { _imagemBase64.value = null }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PublicarOfertaViewModel(context) as T
    }
}

sealed class PublicarEvento {
    data class Sucesso(val msg: String) : PublicarEvento()
    data class Erro(val msg: String) : PublicarEvento()
    data class ValidacaoErro(val campo: String, val msg: String) : PublicarEvento()
}
