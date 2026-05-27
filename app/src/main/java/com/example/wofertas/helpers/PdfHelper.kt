package com.example.wofertas.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import com.example.wofertas.VerPDF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * PdfHelper — utilitário para download e abertura de arquivos PDF.
 *
 * Três modos de uso:
 *
 * 1. Abrir no visualizador interno (VerPDF.kt):
 *    PdfHelper.abrirNoApp(context, url, titulo)
 *
 * 2. Baixar e abrir com leitor externo (Adobe, Drive, etc.):
 *    val file = PdfHelper.baixarPdf(context, url, "encarte.pdf")
 *    PdfHelper.abrirComAppExterno(context, file)
 *
 * 3. Compartilhar PDF:
 *    PdfHelper.compartilhar(context, file)
 *
 * NOTA: Para usar abrirComAppExterno(), adicione o FileProvider no AndroidManifest.xml:
 *
 *   <provider
 *       android:name="androidx.core.content.FileProvider"
 *       android:authorities="${applicationId}.fileprovider"
 *       android:exported="false"
 *       android:grantUriPermissions="true">
 *       <meta-data
 *           android:name="android.support.FILE_PROVIDER_PATHS"
 *           android:resource="@xml/file_provider_paths" />
 *   </provider>
 *
 * E crie res/xml/file_provider_paths.xml:
 *   <?xml version="1.0" encoding="utf-8"?>
 *   <paths>
 *       <cache-path name="pdf_cache" path="pdfs/" />
 *   </paths>
 */
object PdfHelper {

    private const val TAG = "PdfHelper"
    private const val PDF_SUBDIR = "pdfs"

    /**
     * Abre o PDF no visualizador interno (VerPDF.kt) sem download prévio.
     * O VerPDF faz o download internamente.
     */
    fun abrirNoApp(context: Context, url: String, titulo: String = "Encarte") {
        context.startActivity(
            Intent(context, VerPDF::class.java).apply {
                putExtra("pdfUrl",      url)
                putExtra("oferta_nome", titulo)
            }
        )
    }

    /**
     * Baixa o PDF para o diretório de cache do app e retorna o File.
     * Deve ser chamado em uma coroutine (suspend).
     *
     * @param context     contexto da aplicação
     * @param url         URL do PDF (http ou https)
     * @param nomeArquivo nome do arquivo local (ex: "encarte_semana.pdf")
     * @return File se sucesso, null se falhou
     */
    suspend fun baixarPdf(
        context: Context,
        url: String,
        nomeArquivo: String = "encarte_${System.currentTimeMillis()}.pdf"
    ): File? = withContext(Dispatchers.IO) {
        if (!URLUtil.isValidUrl(url)) {
            Log.e(TAG, "URL inválida: $url")
            return@withContext null
        }

        val destino = File(context.cacheDir, PDF_SUBDIR).also { it.mkdirs() }
        val arquivo  = File(destino, nomeArquivo)

        // Reutiliza cache se já existe e tem tamanho válido
        if (arquivo.exists() && arquivo.length() > 0) {
            Log.d(TAG, "PDF em cache: ${arquivo.absolutePath}")
            return@withContext arquivo
        }

        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod    = "GET"
                connectTimeout   = 15_000
                readTimeout      = 30_000
                instanceFollowRedirects = true
                connect()
            }

            if (connection.responseCode !in 200..299) {
                Log.e(TAG, "HTTP ${connection.responseCode} ao baixar PDF")
                connection.disconnect()
                return@withContext null
            }

            connection.inputStream.use { input ->
                FileOutputStream(arquivo).use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()

            Log.d(TAG, "PDF salvo em: ${arquivo.absolutePath} (${arquivo.length()} bytes)")
            arquivo
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar PDF", e)
            arquivo.takeIf { it.exists() }?.delete()
            null
        }
    }

    /**
     * Abre o PDF com o aplicativo leitor instalado no dispositivo (ex: Adobe Reader).
     * Usa FileProvider para compartilhar o arquivo com segurança.
     */
    fun abrirComAppExterno(context: Context, arquivo: File): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                arquivo
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Verifica se há app capaz de abrir PDF
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w(TAG, "Nenhum leitor de PDF instalado")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir PDF externo", e)
            false
        }
    }

    /**
     * Compartilha o PDF via sheet de compartilhamento do Android.
     */
    fun compartilhar(context: Context, arquivo: File, titulo: String = "Encarte") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                arquivo
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, titulo)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar $titulo"))
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao compartilhar PDF", e)
        }
    }

    /**
     * Remove todos os PDFs em cache (útil para liberar espaço).
     */
    fun limparCache(context: Context) {
        File(context.cacheDir, PDF_SUBDIR).listFiles()?.forEach { it.delete() }
        Log.d(TAG, "Cache de PDFs limpo")
    }
}
