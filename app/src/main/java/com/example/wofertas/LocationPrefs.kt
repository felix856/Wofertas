package com.example.wofertas

import android.content.Context
import android.location.Location

/** Persiste a última localização conhecida do usuário para uso no OfertasWorker. */
object LocationPrefs {

    private const val PREFS       = "location_prefs"
    private const val KEY_LAT     = "last_lat"
    private const val KEY_LON     = "last_lon"
    private const val KEY_RAIO    = "raio_metros"
    private const val DEFAULT_RAIO = 10_000f  // 10 km padrão

    fun salvar(context: Context, location: Location) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_LAT, location.latitude.toFloat())
            .putFloat(KEY_LON, location.longitude.toFloat())
            .apply()
    }

    fun getLast(context: Context): Location? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getFloat(KEY_LAT, Float.MIN_VALUE)
        val lon = prefs.getFloat(KEY_LON, Float.MIN_VALUE)
        if (lat == Float.MIN_VALUE) return null
        return Location("").apply { latitude = lat.toDouble(); longitude = lon.toDouble() }
    }

    fun getRaioMetros(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY_RAIO, DEFAULT_RAIO)

    fun setRaioMetros(context: Context, metros: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_RAIO, metros).apply()
    }
}
