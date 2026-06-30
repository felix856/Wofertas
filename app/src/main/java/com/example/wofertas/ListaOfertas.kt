package com.example.wofertas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wofertas.OfertaAdapter.OnOfertaClickListener
import com.example.wofertas.network.ApiClient
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ListaOfertas : BaseClienteActivity(), OnOfertaClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMessage: TextView
    private lateinit var searchView: SearchView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var ofertaAdapter: OfertaAdapter

    private val listaOfertas: MutableList<Oferta> = mutableListOf()
    private val mercadosFavoritados: MutableSet<String> = mutableSetOf()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var ultimaLocalizacao: Location? = null

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var ultimaNotificacao = 0L

    private val api get() = ApiClient.authService(this)

    private val requestPermissionsLauncher = registerForActivityResult(
        RequestMultiplePermissions()
    ) { permissoes ->
        if (permissoes[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startLocationUpdates()
        } else {
            tvMessage.text = "Permissão de localização negada. Ofertas não serão ordenadas por proximidade."
            tvMessage.visibility = View.VISIBLE
        }
        carregarDados()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_ofertas)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_lista_ofertas)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Wofertas"

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBarListaOfertas)
        tvMessage = findViewById(R.id.tvListaOfertasMessage)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ofertaAdapter = OfertaAdapter(mutableListOf(), this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ofertaAdapter

        configurarNavegacao()
        configurarLocalizacao()
        verificarPermissoes()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        carregarDados()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_lista_ofertas, menu)
        val item = menu.findItem(R.id.action_search)
        searchView = item.actionView as SearchView
        configurarBusca()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.activity_perfil) {
            startActivity(Intent(this, Perfil::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun configurarNavegacao() {
        bottomNavigationView.selectedItemId = R.id.navigation_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    atualizarAdapter(listaOfertas)
                    true
                }
                R.id.navigation_mapa -> {
                    Mapa.abrirMapa(this)
                    true
                }
                R.id.navigation_carrinho -> {
                    startActivity(Intent(this, CarrinhoActivity::class.java))
                    true
                }
                R.id.navigation_salvos -> {
                    startActivity(Intent(this, FavoritosActivity::class.java))
                    true
                }
                R.id.navigation_perfil -> {
                    startActivity(Intent(this, Perfil::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun configurarLocalizacao() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.locations.firstOrNull() ?: return
                ultimaLocalizacao = location
                LocationPrefs.salvar(this@ListaOfertas, location)
                verificarProximidade()
                atualizarAdapter(listaOfertas)
            }
        }
    }

    private fun verificarProximidade() {
        val loc = ultimaLocalizacao ?: return
        val agora = System.currentTimeMillis()
        if (agora - ultimaNotificacao > 60000) {
            ProximidadeNotificacao.verificarProximidade(this, loc, listaOfertas)
            ultimaNotificacao = agora
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            } catch (e: SecurityException) {
                Log.e(TAG, "Erro ao iniciar localização", e)
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun verificarPermissoes() {
        val permissoes = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissoes.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissoes.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissoes.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissoes.toTypedArray())
        } else {
            startLocationUpdates()
            carregarDados()
        }
    }

    private fun configurarBusca() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false
            override fun onQueryTextChange(newText: String): Boolean {
                ofertaAdapter.filter.filter(newText)
                return true
            }
        })
    }

    private fun carregarDados() {
        progressBar.visibility = View.VISIBLE
        tvMessage.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val userId = AuthManager.getUserId(this@ListaOfertas)
                if (userId.isNullOrEmpty()) {
                    tratarSessaoExpirada()
                    return@launch
                }
                carregarFavoritos(userId)
                carregarOfertas()
            } catch (e: Exception) {
                Log.e(TAG, "Erro de rede", e)
                mostrarErro("Sem conexão com o servidor.")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun tratarSessaoExpirada() {
        AuthManager.clearSession(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private suspend fun carregarFavoritos(userId: String) {
        val resp = api.listarFavoritos(userId)
        if (resp.isSuccessful) {
            mercadosFavoritados.clear()
            resp.body()?.forEach { mercadosFavoritados.add(it.idMercado) }
        } else if (resp.code() == 401) {
            tratarSessaoExpirada()
        }
    }

    private suspend fun carregarOfertas() {
        val resp = api.listarOfertas()
        if (!resp.isSuccessful) {
            mostrarErro("Erro ao carregar ofertas (${resp.code()}).")
            return
        }
        val dtos = resp.body() ?: emptyList()
        listaOfertas.clear()
        listaOfertas.addAll(
            dtos.map { dto ->
                val mercadoId = dto.mercado?.id ?: dto.mercadoId
                Oferta().apply {
                    ofertaId = dto.id
                    this.mercadoId = mercadoId
                    nome = dto.nome
                    status = dto.status
                    dataValidade = dto.data
                    imagemOferta = dto.imagemOferta ?: dto.imagem
                    nomeSupermercado = dto.mercado?.nome
                    enderecoSupermercado = dto.mercado?.endereco
                    imagemLogo = dto.mercado?.imagemLogo
                    latitude = dto.mercado?.latitude
                    longitude = dto.mercado?.longitude
                    isSaved = mercadoId?.let { mercadosFavoritados.contains(it) } ?: false
                }
            }
        )
        verificarProximidade()
        atualizarAdapter(listaOfertas)
    }

    private fun atualizarAdapter(lista: List<Oferta>) {
        ofertaAdapter.atualizarListaCompleta(ArrayList(lista))
        ofertaAdapter.setUltimaLocalizacaoUsuario(ultimaLocalizacao)
        val query = if (::searchView.isInitialized) searchView.query?.toString() ?: "" else ""
        ofertaAdapter.filter.filter(query)
        val vazio = ofertaAdapter.itemCount == 0
        tvMessage.visibility = if (vazio) View.VISIBLE else View.GONE
        if (vazio) tvMessage.text = "Nenhuma oferta disponível."
    }

    override fun onOfertaClick(oferta: Oferta) {
        val url = oferta.imagemOferta
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "Oferta sem encarte.", Toast.LENGTH_SHORT).show()
            return
        }

        // FIX: Se a URL for Base64 (muito grande), salva no DataHolder em vez da Intent
        if (url.contains("base64,")) {
            DataHolder.bigString = url
        }

        val intent = Intent(this, VerPDF::class.java)
        // Passa a URL apenas se for pequena. Se for grande, o VerPDF pegará do DataHolder
        if (!url.contains("base64,")) {
            intent.putExtra("pdfUrl", url)
        }
        
        intent.putExtra("oferta_nome", oferta.nome)
        intent.putExtra("oferta_id", oferta.ofertaId)
        intent.putExtra("mercado_nome", oferta.nomeSupermercado)

        startActivity(intent)
    }

    override fun onSaveClick(oferta: Oferta, position: Int) {
        val mercadoId = oferta.mercadoId ?: return
        val ofertaId = oferta.ofertaId ?: return
        lifecycleScope.launch {
            try {
                // Curtir a oferta individualmente no backend para alimentar métricas analíticas
                lifecycleScope.launch {
                    try {
                        api.toggleCurtida(ofertaId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao curtir oferta $ofertaId no backend", e)
                    }
                }

                val resp = api.toggleFavorito(mercadoId)
                if (resp.isSuccessful) {
                    if (mercadosFavoritados.contains(mercadoId)) {
                        mercadosFavoritados.remove(mercadoId)
                        oferta.isSaved = false
                    } else {
                        mercadosFavoritados.add(mercadoId)
                        oferta.isSaved = true
                    }
                    ofertaAdapter.notifyItemChanged(position)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao favoritar", e)
            }
        }
    }

    private fun mostrarErro(msg: String) {
        tvMessage.text = msg
        tvMessage.visibility = View.VISIBLE
    }

    companion object {
        private const val TAG = "ListaOfertas"
    }
}
