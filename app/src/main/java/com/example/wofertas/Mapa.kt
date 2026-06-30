package com.example.wofertas

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.wofertas.network.ApiClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class Mapa : BaseClienteActivity() {

    private lateinit var mapView: MapView
    private lateinit var fabLocalizacao: FloatingActionButton
    private lateinit var fabLista: FloatingActionButton
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var locationOverlay: MyLocationNewOverlay

    private val listaOfertas = mutableListOf<Oferta>()
    private val api get() = ApiClient.authService(this)
    private val raioOfertasKm = 30.0

    // ✅ NOVO: Gerenciador de permissões moderno
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            configurarLocalizacao()
        } else {
            Toast.makeText(this, "Permissão de localização negada. O mapa não mostrará sua posição.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            applicationContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )

        setContentView(R.layout.activity_mapa)

        mapView = findViewById(R.id.map)
        fabLocalizacao = findViewById(R.id.fabMinhaLocalizacao)
        fabLista = findViewById(R.id.fabListaOfertas)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        findViewById<Toolbar>(R.id.toolbar_mapa).setNavigationOnClickListener { finish() }

        configurarMapa()
        verificarPermissoesEConfigurarLocalizacao()
        configurarBotoes()
        configurarNavegacao()
        carregarOfertas()
    }

    private fun configurarMapa() {
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(14.0)
        // Centro padrão: Palhoça/SC
        val centro = GeoPoint(-27.6453, -48.6693)
        mapView.controller.setCenter(centro)
    }

    // ✅ NOVO: Lógica de verificação de permissões
    private fun verificarPermissoesEConfigurarLocalizacao() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            configurarLocalizacao()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun configurarLocalizacao() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation() // Opcional: segue o usuário ao abrir
        mapView.overlays.add(locationOverlay)
        mapView.postDelayed({
            locationOverlay.myLocation?.let { geo ->
                val location = android.location.Location("").apply {
                    latitude = geo.latitude
                    longitude = geo.longitude
                }
                LocationPrefs.salvar(this, location)
                carregarOfertas()
            }
        }, 2000)
    }

    private fun carregarOfertas() {
        lifecycleScope.launch {
            try {
                val localizacao = LocationPrefs.getLast(this@Mapa)
                val resp = if (localizacao != null) {
                    api.listarOfertasProximas(
                        lat = localizacao.latitude,
                        lng = localizacao.longitude,
                        raioKm = raioOfertasKm,
                        ativo = true
                    )
                } else {
                    api.listarOfertas(size = 100, ativo = true)
                }
                if (resp.isSuccessful) {
                    val dtos = resp.body() ?: emptyList()
                    listaOfertas.clear()
                    listaOfertas.addAll(dtos.map { dto ->
                        Oferta().apply {
                            ofertaId = dto.id
                            mercadoId = dto.mercado?.id ?: dto.mercadoId
                            nome = dto.nome
                            status = dto.status
                            dataValidade = dto.data
                            imagemOferta = dto.imagem ?: dto.imagemOferta
                            nomeSupermercado = dto.mercado?.nome
                            enderecoSupermercado = dto.mercado?.endereco
                            latitude = dto.mercado?.latitude
                            longitude = dto.mercado?.longitude
                        }
                    })
                    adicionarMarcadores()
                    if (listaOfertas.none { it.latitude != null && it.longitude != null }) {
                        Toast.makeText(this@Mapa, "Nenhuma oferta ativa com localizacao no mapa.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@Mapa, "Erro ao carregar ofertas (${resp.code()}).", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Mapa, "Erro de conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun adicionarMarcadores() {
        mapView.overlays.removeAll { it is Marker }

        listaOfertas.groupBy { it.mercadoId }.forEach { (_, ofertas) ->
            val principal = ofertas.first()
            val lat = principal.latitude
            val lon = principal.longitude

            if (lat != null && lon != null) {
                val marker = Marker(mapView)
                marker.position = GeoPoint(lat, lon)
                marker.title = principal.nomeSupermercado
                marker.subDescription = if (ofertas.size > 1) "${ofertas.size} ofertas aqui" else principal.nome
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                marker.setOnMarkerClickListener { _, _ ->
                    abrirEncarteDoMercado(principal)
                    true
                }
                mapView.overlays.add(marker)
            }
        }
        mapView.invalidate()
    }

    private fun abrirEncarteDoMercado(oferta: Oferta) {
        val mercadoId = oferta.mercadoId
        if (mercadoId.isNullOrBlank()) {
            Toast.makeText(this, "Mercado da oferta nao identificado.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                Toast.makeText(this@Mapa, "Carregando encarte...", Toast.LENGTH_SHORT).show()
                val resp = api.getEncartesByMercado(mercadoId)
                if (!resp.isSuccessful) {
                    Toast.makeText(this@Mapa, "Erro ao carregar encartes (${resp.code()}).", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val encarte = resp.body()?.firstOrNull()
                if (encarte == null) {
                    Toast.makeText(this@Mapa, "Este mercado ainda nao tem encarte publicado.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                if (encarte.urlPdf.isBlank()) {
                    Toast.makeText(this@Mapa, "Encarte sem arquivo publicado.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                startActivity(Intent(this@Mapa, VerPDF::class.java).apply {
                    putExtra("pdfUrl", normalizarUrlEncarte(encarte.urlPdf))
                    putExtra("oferta_id", encarte.id)
                    putExtra("oferta_nome", encarte.titulo)
                    putExtra("mercado_nome", oferta.nomeSupermercado ?: "Mercado")
                })
            } catch (e: Exception) {
                Toast.makeText(this@Mapa, "Falha ao abrir encarte.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun normalizarUrlEncarte(urlPdf: String): String {
        if (urlPdf.startsWith("http", ignoreCase = true)) return urlPdf
        val baseUrl = ApiClient.getCurrentBaseUrl().removeSuffix("/")
        val path = if (urlPdf.startsWith("/")) urlPdf else "/$urlPdf"
        return baseUrl + path
    }

    private fun configurarBotoes() {
        fabLocalizacao.setOnClickListener {
            val location = if(::locationOverlay.isInitialized) locationOverlay.myLocation else null
            if (location != null) {
                mapView.controller.animateTo(location)
                mapView.controller.setZoom(17.0)
            } else {
                Toast.makeText(this, "Aguardando GPS...", Toast.LENGTH_SHORT).show()
            }
        }
        fabLista.setOnClickListener { finish() }
    }

    private fun configurarNavegacao() {
        bottomNavigationView.selectedItemId = R.id.navigation_mapa
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { finish(); true }
                R.id.navigation_mapa -> true
                R.id.navigation_carrinho -> { startActivity(Intent(this, CarrinhoActivity::class.java)); true }
                R.id.navigation_salvos -> { startActivity(Intent(this, FavoritosActivity::class.java)); true }
                R.id.navigation_perfil -> { startActivity(Intent(this, Perfil::class.java)); true }
                else -> false
            }
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }

    companion object {
        fun abrirMapa(context: Context) {
            context.startActivity(Intent(context, Mapa::class.java))
        }
    }
}
