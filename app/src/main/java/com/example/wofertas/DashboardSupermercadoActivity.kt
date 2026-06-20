package com.example.wofertas

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.BackendErrorDto
import com.example.wofertas.network.DashboardAnalyticsDto
import com.example.wofertas.network.MercadoRankingDto
import com.example.wofertas.network.OfertaRequest
import com.example.wofertas.ui.adapters.CompetidorRankingAdapter
import com.example.wofertas.ui.encartes.EncartesActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard do Supermercado para publicação e atalhos de gestão.
 */
class DashboardSupermercadoActivity : BaseMercadoActivity() {

    private lateinit var etNomeOferta:       EditText
    private lateinit var etStatusOferta:     EditText
    private lateinit var etDataOferta:       EditText
    private lateinit var tvImagemNome:       TextView
    private lateinit var btnSelecionarImagem: Button
    private lateinit var btnPublicarOferta:  Button
    private lateinit var progressBar:        ProgressBar
    private lateinit var ivPreview:          ImageView
    private lateinit var bottomNav:          BottomNavigationView
    private lateinit var tvInsightsStatus:   TextView
    private lateinit var progressInsights:   ProgressBar
    private lateinit var tvInsightsError:    TextView
    private lateinit var tvMetricsViews:     TextView
    private lateinit var tvMetricsEngagement: TextView
    private lateinit var tvMetricsCart:      TextView
    private lateinit var tvMetricsRank:      TextView
    private lateinit var btnVerInsightsDetalhados: Button

    private var imagemUri: Uri? = null
    private var ultimoDashboard: DashboardAnalyticsDto? = null
    private var ultimoRanking: List<MercadoRankingDto> = emptyList()
    private val api get() = ApiClient.authService(this)

    private val formatExibicao = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val formatServidor = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val selecionarImagem = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imagemUri = uri
            tvImagemNome.text = nomeArquivo(uri)
            tvImagemNome.visibility = View.VISIBLE
            ivPreview.setImageURI(uri)
            ivPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_supermercado)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_dashboard)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))

        etNomeOferta         = findViewById(R.id.etTituloOferta)
        etStatusOferta       = findViewById(R.id.etDescricaoOferta)
        etDataOferta         = findViewById(R.id.etScheduledDate)
        tvImagemNome         = findViewById(R.id.tvPdfName)
        btnSelecionarImagem  = findViewById(R.id.btnSelectPdf)
        btnPublicarOferta    = findViewById(R.id.btnPublishOffer)
        progressBar          = findViewById(R.id.progressBarUpload)
        ivPreview            = findViewById(R.id.ivPreviewDashboard)
        bottomNav            = findViewById(R.id.bottom_navigation_supermercado)
        tvInsightsStatus     = findViewById(R.id.tvInsightsStatus)
        progressInsights     = findViewById(R.id.progressInsights)
        tvInsightsError      = findViewById(R.id.tvInsightsError)
        tvMetricsViews       = findViewById(R.id.tvMetricsViews)
        tvMetricsEngagement  = findViewById(R.id.tvMetricsEngagement)
        tvMetricsCart        = findViewById(R.id.tvMetricsCart)
        tvMetricsRank        = findViewById(R.id.tvMetricsRank)
        btnVerInsightsDetalhados = findViewById(R.id.btnVerInsightsDetalhados)

        etDataOferta.isFocusable = false
        etDataOferta.setOnClickListener { mostrarDatePicker() }

        btnSelecionarImagem.setOnClickListener { selecionarImagem.launch("image/*") }
        btnPublicarOferta.setOnClickListener   { validarEPublicar() }
        btnVerInsightsDetalhados.setOnClickListener { abrirInsightsDetalhados() }

        configurarBottomNav()
        carregarInsights()
    }

    private fun carregarInsights() {
        progressInsights.visibility = View.VISIBLE
        tvInsightsError.visibility = View.GONE
        tvInsightsStatus.text = "Carregando metricas do mercado"
        btnVerInsightsDetalhados.isEnabled = false

        lifecycleScope.launch {
            try {
                val dashboardResp = api.getDashboardAnalytics()
                val rankingResp = api.getRankingMercados()

                if (dashboardResp.isSuccessful && rankingResp.isSuccessful) {
                    ultimoDashboard = dashboardResp.body()
                    ultimoRanking = rankingResp.body().orEmpty()
                    atualizarCardInsights()
                    tvInsightsStatus.text = "Dados atualizados"
                    btnVerInsightsDetalhados.isEnabled = true
                } else {
                    val code = if (!dashboardResp.isSuccessful) dashboardResp.code() else rankingResp.code()
                    mostrarErroInsights("Nao foi possivel carregar os insights ($code).")
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Erro ao carregar insights", e)
                mostrarErroInsights("Sem conexao com as metricas. Verifique o backend.")
            } finally {
                progressInsights.visibility = View.GONE
            }
        }
    }

    private fun atualizarCardInsights() {
        val dashboard = ultimoDashboard ?: return
        val meuId = AuthManager.getUserId(this)
        val meuRanking = ultimoRanking.firstOrNull { it.id == meuId }

        tvMetricsViews.text = dashboard.totalVisualizacoes.toString()
        tvMetricsEngagement.text = "${dashboard.totalCurtidas} curtidas + ${dashboard.totalFavoritos} salvos"
        tvMetricsCart.text = "${dashboard.totalItensCarrinho} itens"
        tvMetricsRank.text = meuRanking?.let { "#${it.posicao} no ranking" } ?: "Sem posicao"
    }

    private fun mostrarErroInsights(mensagem: String) {
        tvInsightsStatus.text = "Metricas indisponiveis"
        tvInsightsError.text = mensagem
        tvInsightsError.visibility = View.VISIBLE
        btnVerInsightsDetalhados.isEnabled = ultimoDashboard != null || ultimoRanking.isNotEmpty()
    }

    private fun abrirInsightsDetalhados() {
        val dashboard = ultimoDashboard
        val ranking = ultimoRanking

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_insights_detalhados, null)
        dialog.setContentView(view)

        val recomendacao = view.findViewById<TextView>(R.id.tvAiInsightRecommendation)
        val clientesAtivos = view.findViewById<TextView>(R.id.tvAiInsightActiveClients)
        val melhorOferta = view.findViewById<TextView>(R.id.tvInsightBestOffer)
        val tendencia = view.findViewById<TextView>(R.id.tvInsightTrend)
        val vazio = view.findViewById<TextView>(R.id.tvRankingEmpty)
        val recycler = view.findViewById<RecyclerView>(R.id.rvCompetidoresRanking)

        val insight = dashboard?.insight
        recomendacao.text = insight?.recomendacao ?: "Publique ofertas para gerar recomendacoes inteligentes."
        clientesAtivos.text = "${insight?.clientesAtivos ?: 0} clientes ativos"
        melhorOferta.text = "Top: ${insight?.encarteMelhorPerformance ?: "sem dados"}"
        tendencia.text = insight?.tendencia ?: "Sem tendencia suficiente por enquanto"

        val adapter = CompetidorRankingAdapter(AuthManager.getUserId(this))
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        adapter.submitList(ranking)

        vazio.visibility = if (ranking.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility = if (ranking.isEmpty()) View.GONE else View.VISIBLE

        dialog.show()
    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(this, { _, y, m, d ->
            val selCal = Calendar.getInstance().apply { set(y, m, d) }
            etDataOferta.setText(formatExibicao.format(selCal.time))
        }, year, month, day)
        
        dpd.show()
    }

    private fun validarEPublicar() {
        val nome   = etNomeOferta.text.toString().trim()
        val status = etStatusOferta.text.toString().trim().uppercase().ifEmpty { "ATIVO" }
        val dataStr = etDataOferta.text.toString().trim()

        if (nome.isEmpty()) { etNomeOferta.error = "Informe o nome"; return }
        if (dataStr.isEmpty()) { etDataOferta.error = "Selecione a validade"; return }
        
        val dataParaServidor = try {
            val date = formatExibicao.parse(dataStr)
            formatServidor.format(date!!)
        } catch (e: Exception) {
            etDataOferta.error = "Data inválida"; return
        }

        if (imagemUri == null) { 
            Toast.makeText(this, "Selecione uma imagem", Toast.LENGTH_SHORT).show()
            return 
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                // 1. Criar a oferta
                val resp = api.criarOferta(OfertaRequest(nome, status, dataParaServidor, null))
                
                if (resp.isSuccessful) {
                    val ofertaCriada = resp.body()
                    if (ofertaCriada != null) {
                        uploadImagem(ofertaCriada.id)
                    }
                } else {
                    val errorBody = resp.errorBody()?.string()
                    Log.e("Dashboard", "Erro ao criar: $errorBody")
                    Toast.makeText(this@DashboardSupermercadoActivity, "Erro no servidor (${resp.code()})", Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Falha de conexão", e)
                Toast.makeText(this@DashboardSupermercadoActivity, "Erro de conexão", Toast.LENGTH_SHORT).show()
                setLoading(false)
            }
        }
    }

    private suspend fun uploadImagem(ofertaId: String) {
        try {
            val file = withContext(Dispatchers.IO) { prepararImagem(imagemUri!!) }
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("foto", file.name, requestFile)

            val resp = api.uploadImagemOferta(ofertaId, part)
            if (resp.isSuccessful) {
                Toast.makeText(this, "Oferta publicada com sucesso!", Toast.LENGTH_SHORT).show()
                limparUI()
                startActivity(Intent(this, GerenciarOfertasSupermercadoActivity::class.java))
                finish()
            } else {
                Log.e("Dashboard", "Erro upload imagem: ${resp.errorBody()?.string()}")
                Toast.makeText(this, "Oferta criada, mas falha no upload da imagem.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("Dashboard", "Erro no upload", e)
            Toast.makeText(this, "Falha ao processar imagem", Toast.LENGTH_SHORT).show()
        } finally {
            setLoading(false)
        }
    }

    private fun prepararImagem(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val file = File(cacheDir, "oferta_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
        }
        return file
    }

    private fun nomeArquivo(uri: Uri): String {
        var result = "imagem.jpg"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1) result = cursor.getString(idx)
            }
        }
        return result
    }

    private fun limparUI() {
        etNomeOferta.text.clear()
        etStatusOferta.text.clear()
        etDataOferta.text.clear()
        tvImagemNome.visibility = View.GONE
        ivPreview.visibility = View.GONE
        imagemUri = null
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnPublicarOferta.isEnabled = !loading
    }

    private fun configurarBottomNav() {
        bottomNav.selectedItemId = R.id.navigation_dashboard
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> true
                R.id.navigation_minhas_ofertas,
                R.id.navigation_gerenciar_ofertas_supermercado -> {
                    startActivity(Intent(this, GerenciarOfertasSupermercadoActivity::class.java))
                    true
                }
                R.id.navigation_encartes -> {
                    abrirMeusEncartes()
                    true
                }
                R.id.navigation_perfil_supermercado -> {
                    startActivity(Intent(this, Perfil::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun abrirMeusEncartes() {
        val mercadoId = AuthManager.getUserId(this) ?: return
        startActivity(Intent(this, EncartesActivity::class.java).apply {
            putExtra("mercado_id", mercadoId)
            putExtra("mercado_nome", AuthManager.getNome(this@DashboardSupermercadoActivity) ?: "Meus encartes")
        })
    }
}
