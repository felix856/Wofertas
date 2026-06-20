package com.example.wofertas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.data.local.AppDatabase
import com.example.wofertas.data.local.entities.ProdutoListaEntity
import com.example.wofertas.fcm.NotificationHelper
import com.example.wofertas.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import androidx.core.graphics.createBitmap

class VerPDF : AppCompatActivity() {

    private lateinit var imageViewPdfPage:       ImageView
    private lateinit var btnPaginaAnterior:      Button
    private lateinit var btnProximaPagina:       Button
    private lateinit var textViewContadorPagina: TextView
    private lateinit var progressBar:            ProgressBar
    private lateinit var tvOcrStatus:            TextView

    private var pdfUrl:      String? = null
    private var ofertaId:    String  = ""
    private var mercadoNome: String  = ""

    private var pdfRenderer:          PdfRenderer?        = null
    private var currentPage:          PdfRenderer.Page?   = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentPageIndex = 0

    private val base64Bitmaps = mutableListOf<Bitmap>()
    private val paginasComOcrFeito = mutableSetOf<Int>()
    private val db by lazy { AppDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_pdf)

        // RECUPERAÇÃO SEGURA: Prioriza DataHolder para imagens grandes (Base64)
        pdfUrl = intent.getStringExtra("pdfUrl")
        if (pdfUrl.isNullOrBlank()) {
            pdfUrl = DataHolder.bigString
        }
        
        ofertaId    = intent.getStringExtra("oferta_id") ?: ""
        mercadoNome = intent.getStringExtra("mercado_nome") ?: ""
        val titulo  = intent.getStringExtra("oferta_nome") ?: "Encarte da Oferta"

        initViews(titulo)
        NotificationHelper.criarCanais(this)
        
        if (!pdfUrl.isNullOrBlank()) {
            carregarEncarte(pdfUrl!!)
        } else {
            handleError("Encarte não encontrado.")
        }
    }

    private fun initViews(titulo: String) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_ver_pdf)
        setSupportActionBar(toolbar)
        supportActionBar?.title = titulo
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        imageViewPdfPage       = findViewById(R.id.imageViewPdfPage)
        btnPaginaAnterior      = findViewById(R.id.btnPaginaAnterior)
        btnProximaPagina       = findViewById(R.id.btnProximaPagina)
        textViewContadorPagina = findViewById(R.id.textViewContadorPagina)
        progressBar            = findViewById(R.id.progressBarVerPDF)
        tvOcrStatus            = findViewById(R.id.tvOcrStatus)

        btnPaginaAnterior.setOnClickListener { navegarPagina(currentPageIndex - 1) }
        btnProximaPagina.setOnClickListener  { navegarPagina(currentPageIndex + 1) }
    }

    private fun carregarEncarte(fonte: String) {
        progressBar.visibility = View.VISIBLE
        if (fonte.contains("base64,")) {
            carregarBase64(fonte)
        } else {
            val urlFinal = if (!fonte.startsWith("http")) {
                ApiClient.getCurrentBaseUrl().removeSuffix("/") + "/" + fonte.removePrefix("/")
            } else fonte
            downloadAndOpenPdf(urlFinal)
        }
    }

    private fun carregarBase64(base64: String) {
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val pureData = if (base64.contains(",")) base64.substringAfter(",") else base64
                    val bytes = Base64.decode(pureData, Base64.DEFAULT)
                    
                    // Decodificação Segura: Redimensiona se for maior que 2000px para não dar tela bege
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                        inSampleSize = if (outWidth > 2000 || outHeight > 2000) 2 else 1
                        inJustDecodeBounds = false
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                } catch (e: Exception) { 
                    Log.e("VerPDF", "Erro ao decodificar Base64", e)
                    null 
                }
            }
            
            progressBar.visibility = View.GONE
            if (bitmap != null) {
                base64Bitmaps.clear()
                base64Bitmaps.add(bitmap)
                mostrarBitmapDireto(0)
            } else {
                handleError("Não foi possível carregar a imagem do encarte.")
            }
        }
    }

    private fun downloadAndOpenPdf(urlStr: String) {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    val connection = URL(urlStr).openConnection() as HttpURLConnection
                    conn = connection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("Accept", "application/pdf,image/*,*/*")
                    val statusCode = connection.responseCode
                    if (statusCode !in 200..299) {
                        throw IOException("HTTP $statusCode ao baixar encarte")
                    }
                    val temp = File(cacheDir, "temp_encarte.dat")
                    connection.inputStream.use { input -> FileOutputStream(temp).use { output -> input.copyTo(output) } }
                    temp
                } catch (e: Exception) { 
                    Log.e("VerPDF", "Erro no download: $urlStr", e)
                    null 
                } finally {
                    conn?.disconnect()
                }
            }
            progressBar.visibility = View.GONE
            if (file != null) abrirArquivo(file)
            else handleError("Falha ao baixar o encarte.")
        }
    }

    private fun abrirArquivo(file: File) {
        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            showPdfPage(0)
        } catch (e: Exception) {
            // Se falhar como PDF, tenta como imagem comum
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                base64Bitmaps.clear()
                base64Bitmaps.add(bitmap)
                mostrarBitmapDireto(0)
            } else handleError("Arquivo de encarte inválido ou corrompido.")
        }
    }

    private fun showPdfPage(index: Int) {
        val renderer = pdfRenderer ?: return
        if (index < 0 || index >= renderer.pageCount) return
        try {
            currentPage?.close()
            currentPage = renderer.openPage(index)
            val bitmap = createBitmap(currentPage!!.width, currentPage!!.height)
            bitmap.eraseColor(Color.WHITE)
            currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            imageViewPdfPage.setImageBitmap(bitmap)
            currentPageIndex = index
            atualizarBotoes(renderer.pageCount)
            executarOcrNaPagina(bitmap, index)
        } catch (e: Exception) {
            Log.e("VerPDF", "Erro ao renderizar PDF", e)
            handleError("Nao foi possivel renderizar este encarte.")
        }
    }

    private fun mostrarBitmapDireto(index: Int) {
        if (base64Bitmaps.isEmpty()) return
        imageViewPdfPage.setImageBitmap(base64Bitmaps[index])
        currentPageIndex = index
        atualizarBotoes(base64Bitmaps.size)
        executarOcrNaPagina(base64Bitmaps[index], index)
    }

    private fun navegarPagina(index: Int) {
        if (pdfRenderer != null) showPdfPage(index)
        else if (base64Bitmaps.isNotEmpty()) mostrarBitmapDireto(index)
    }

    private fun executarOcrNaPagina(bitmap: Bitmap, pageIndex: Int) {
        if (pageIndex in paginasComOcrFeito) return
        paginasComOcrFeito.add(pageIndex)
        tvOcrStatus.visibility = View.VISIBLE
        tvOcrStatus.text = "Identificando ofertas..."
        
        lifecycleScope.launch {
            val produtosEncontrados = withContext(Dispatchers.Default) { OcrHelper.extrairDeBitmap(bitmap) }
            val listaDoUsuario = withContext(Dispatchers.IO) { db.produtoListaDao().getAll() }
            
            if (produtosEncontrados.isNotEmpty()) {
                val resumo = produtosEncontrados.take(3).joinToString(", ") { it.nome }
                tvOcrStatus.text = "Identificado: $resumo..."
                
                // LÓGICA DE MATCH REVISADA (Sem variáveis ambíguas)
                listaDoUsuario.forEach { itemDaLista ->
                    val matchOcr = produtosEncontrados.firstOrNull { prodOcr -> 
                        OcrHelper.corresponde(itemDaLista.nome, prodOcr.nome) 
                    }
                    
                    if (matchOcr != null) {
                        val precoTxt = matchOcr.preco?.let { v -> " por R$ %.2f".format(Locale("pt", "BR"), v) } ?: ""
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@VerPDF, "Match! Achamos seu ${itemDaLista.nome}!", Toast.LENGTH_SHORT).show()
                        }
                        
                        NotificationHelper.notificarProdutoEncontrado(
                            this@VerPDF, 
                            itemDaLista.nome, 
                            mercadoNome.ifBlank { "Santo Amaro" }, 
                            precoTxt, 
                            (ofertaId + itemDaLista.id + matchOcr.nome).hashCode()
                        )
                    }
                }
            } else {
                tvOcrStatus.text = "Página lida."
            }
        }
    }

    private fun atualizarBotoes(total: Int) {
        btnPaginaAnterior.isEnabled = currentPageIndex > 0
        btnProximaPagina.isEnabled  = currentPageIndex < total - 1
        textViewContadorPagina.text = "Página ${currentPageIndex + 1} / $total"
    }

    private fun handleError(msg: String) {
        progressBar.visibility = View.GONE
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            currentPage?.close(); pdfRenderer?.close(); parcelFileDescriptor?.close()
        } catch (_: Exception) {}
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
