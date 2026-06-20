package com.example.wofertas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.wofertas.data.local.dao.*
import com.example.wofertas.data.local.entities.*

@Database(
    entities = [
        OfertaEntity::class,
        MercadoEntity::class,
        FavoritoEntity::class,
        EncarteEntity::class,
        ProdutoListaEntity::class,
        ProdutoEncontradoEntity::class
    ],
    version = 3,   // FIX: Aumentado para 3 para forçar migração
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ofertaDao(): OfertaDao
    abstract fun mercadoDao(): MercadoDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun encarteDao(): EncarteDao
    abstract fun produtoListaDao(): ProdutoListaDao
    abstract fun produtoEncontradoDao(): ProdutoEncontradoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wofertas_database_final" // FIX: Nome novo para garantir limpeza total
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
