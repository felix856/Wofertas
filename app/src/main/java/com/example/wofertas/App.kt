package com.example.wofertas

import android.app.Application
import androidx.work.*
import com.example.wofertas.fcm.NotificationHelper
import com.jakewharton.threetenabp.AndroidThreeTen
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        NotificationHelper.criarCanais(this)
        agendarWorker()
    }

    private fun agendarWorker() {
        val request = PeriodicWorkRequestBuilder<OfertasWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            OfertasWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // não reinicia se já estiver agendado
            request
        )
    }
}