package com.example.wofertas.data.local.dao

import androidx.room.*
import com.example.wofertas.data.local.entities.*
import kotlinx.coroutines.flow.Flow

// ─── OFERTA ───────────────────────────────────────────────────────────────────
@Dao
interface OfertaDao {
    @Query("SELECT * FROM ofertas_cache ORDER BY cachedAt DESC")
    fun getAllFlow(): Flow<List<OfertaEntity>>

    @Query("SELECT * FROM ofertas_cache WHERE status = 'ATIVO' ORDER BY cachedAt DESC")
    suspend fun getAtivas(): List<OfertaEntity>

    @Query("SELECT * FROM ofertas_cache WHERE mercadoId = :mercadoId ORDER BY cachedAt DESC")
    suspend fun getByMercado(mercadoId: String): List<OfertaEntity>

    @Query("SELECT * FROM ofertas_cache WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OfertaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ofertas: List<OfertaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(oferta: OfertaEntity)

    @Delete
    suspend fun delete(oferta: OfertaEntity)

    @Query("DELETE FROM ofertas_cache WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ofertas_cache")
    suspend fun clearAll()

    @Query("SELECT * FROM ofertas_cache WHERE cachedAt > :since ORDER BY cachedAt DESC")
    suspend fun getFreshCache(since: Long): List<OfertaEntity>
}

// ─── MERCADO ──────────────────────────────────────────────────────────────────
@Dao
interface MercadoDao {
    @Query("SELECT * FROM mercados_cache ORDER BY nome ASC")
    fun getAllFlow(): Flow<List<MercadoEntity>>

    @Query("SELECT * FROM mercados_cache ORDER BY nome ASC")
    suspend fun getAll(): List<MercadoEntity>

    @Query("SELECT * FROM mercados_cache WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MercadoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mercados: List<MercadoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mercado: MercadoEntity)

    @Query("DELETE FROM mercados_cache")
    suspend fun clearAll()
}

// ─── FAVORITO ─────────────────────────────────────────────────────────────────
@Dao
interface FavoritoDao {
    @Query("SELECT * FROM favoritos_cache WHERE usuarioId = :usuarioId")
    fun getAllByUserFlow(usuarioId: String): Flow<List<FavoritoEntity>>

    @Query("SELECT * FROM favoritos_cache WHERE usuarioId = :usuarioId")
    suspend fun getAllByUser(usuarioId: String): List<FavoritoEntity>

    @Query("SELECT COUNT(*) > 0 FROM favoritos_cache WHERE usuarioId = :usuarioId AND mercadoId = :mercadoId")
    suspend fun isFavorito(usuarioId: String, mercadoId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favoritos: List<FavoritoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorito: FavoritoEntity)

    @Query("DELETE FROM favoritos_cache WHERE usuarioId = :usuarioId AND mercadoId = :mercadoId")
    suspend fun delete(usuarioId: String, mercadoId: String)

    @Query("DELETE FROM favoritos_cache WHERE usuarioId = :usuarioId")
    suspend fun clearByUser(usuarioId: String)

    @Query("SELECT * FROM favoritos_cache WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<FavoritoEntity>
}

// ─── ENCARTE ──────────────────────────────────────────────────────────────────
@Dao
interface EncarteDao {
    @Query("SELECT * FROM encartes_cache WHERE mercadoId = :mercadoId ORDER BY dataCriacao DESC")
    fun getByMercadoFlow(mercadoId: String): Flow<List<EncarteEntity>>

    @Query("SELECT * FROM encartes_cache WHERE mercadoId = :mercadoId ORDER BY dataCriacao DESC")
    suspend fun getByMercado(mercadoId: String): List<EncarteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(encartes: List<EncarteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(encarte: EncarteEntity)

    @Query("DELETE FROM encartes_cache WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM encartes_cache WHERE mercadoId = :mercadoId")
    suspend fun clearByMercado(mercadoId: String)
}

// ─── LISTA DE COMPRAS (Carrinho / OCR) ────────────────────────────────────────
@Dao
interface ProdutoListaDao {
    @Query("SELECT * FROM lista_compras ORDER BY nome ASC")
    fun getAllFlow(): Flow<List<ProdutoListaEntity>>

    @Query("SELECT * FROM lista_compras ORDER BY nome ASC")
    suspend fun getAll(): List<ProdutoListaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produto: ProdutoListaEntity)

    @Delete
    suspend fun delete(produto: ProdutoListaEntity)

    @Query("DELETE FROM lista_compras WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM lista_compras")
    suspend fun clearAll()

    @Query("UPDATE lista_compras SET precoMaximo = :preco WHERE id = :id")
    suspend fun updatePrecoMaximo(id: Long, preco: Double?)
}

@Dao
interface ProdutoEncontradoDao {
    @Query("SELECT * FROM produtos_encontrados ORDER BY encontradoEm DESC")
    suspend fun getAll(): List<ProdutoEncontradoEntity>

    @Query("SELECT * FROM produtos_encontrados WHERE nomeProduto = :nome ORDER BY preco ASC")
    suspend fun getByNome(nome: String): List<ProdutoEncontradoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(produtos: List<ProdutoEncontradoEntity>)

    @Query("DELETE FROM produtos_encontrados")
    suspend fun clearAll()

    @Query("DELETE FROM produtos_encontrados WHERE encontradoEm < :antes")
    suspend fun deleteOlderThan(antes: Long)
}