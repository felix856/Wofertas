package com.example.wofertas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class Mapa extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<Oferta> listaOfertasRecebida;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        if (getIntent().hasExtra("lista_ofertas")) {
            listaOfertasRecebida = (ArrayList<Oferta>) getIntent().getSerializableExtra("lista_ofertas");
        }
        if (listaOfertasRecebida == null) {
            listaOfertasRecebida = new ArrayList<>();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (listaOfertasRecebida.isEmpty()) {
            LatLng localPadrao = new LatLng(-27.6479, -48.6718);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(localPadrao, 12f));
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasMarkers = false;

        for (Oferta oferta : listaOfertasRecebida) {
            if (oferta.getLatitude() != null && oferta.getLongitude() != null) {
                LatLng localSupermercado = new LatLng(oferta.getLatitude(), oferta.getLongitude());

                // CORRIGIDO: Usa getNome() e getDescricao() para o marcador
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(localSupermercado)
                        .title(oferta.getNome())
                        .snippet(oferta.getDescricao()));

                if (marker != null) {
                    marker.setTag(oferta);
                    boundsBuilder.include(localSupermercado);
                    hasMarkers = true;
                }
            }
        }

        if (hasMarkers) {
            LatLngBounds bounds = boundsBuilder.build();
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } else {
            LatLng localPadrao = new LatLng(-27.6479, -48.6718);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(localPadrao, 12f));
        }

        mMap.setOnInfoWindowClickListener(clickedMarker -> {
            Oferta ofertaSelecionada = (Oferta) clickedMarker.getTag();
            if (ofertaSelecionada != null && ofertaSelecionada.getUrlPdf() != null) {
                Intent intent = new Intent(Mapa.this, VerPDF.class);
                intent.putExtra("oferta_pdf_url", ofertaSelecionada.getUrlPdf());
                intent.putExtra("oferta_nome", ofertaSelecionada.getNome());
                startActivity(intent);
            }
        });
    }
}
