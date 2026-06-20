package com.example.wofertas.data.repository

import android.content.Context
import com.example.wofertas.Oferta
import com.example.wofertas.data.local.AppDatabase
import com.example.wofertas.data.local.entities.OfertaEntity
import com.example.wofertas.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Gerencia dados de ofertas seguindo Seção 1.2 e 1.3 da especificação.
 */
class OfertaRepository(private val context: Context) {

    private val api get() = ApiClient.authService(context)
    private val db = AppDatabase.getInstance(context)
    private val dao = db.ofertaDao()

    /** Flow de cache local — conforme Seção 7.1 */
    val ofertasFlow: Flow<List<OfertaEntity>> = dao.getAllFlow()

    /**
     * Lista ofertas seguindo Seção 1.2 (Online First, Offline Fallback).
     */
    suspend fun listarOfertas(page: Int = 0, size: Int = 20): ApiResult<List<OfertaResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = api.listarOfertas(page, size, true)
            if (response.isSuccessful) {
                val remote = response.body() ?: emptyList()
                // Atualiza cache Room conforme Seção 7.3
                dao.insertAll(remote.map { it.toEntity() })
                ApiResult.Success(remote)
            } else {
                ApiResult.Error(Exception("Erro ${response.code()}"))
            }
        } catch (e: Exception) {
            // Fallback para cache local conforme Seção 7.3
            val cached = dao.getAtivas()
            if (cached.isNotEmpty()) {
                ApiResult.Success(cached.map { it.toResponse() })
            } else {
                ApiResult.Error(e)
            }
        }
    }

    suspend fun fetchOfertas(): Result<List<Oferta>> = withContext(Dispatchers.IO) {
        try {
            val resp = api.listarOfertas()
            if (resp.isSuccessful) {
                val dtos = resp.body() ?: emptyList()
                val ofertas = dtos.map { it.toModel() }
                Result.success(ofertas)
            } else {
                Result.failure(Exception("Erro ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMinhasOfertas(): Result<List<Oferta>> = withContext(Dispatchers.IO) {
        try {
            val resp = api.listarMinhasOfertas()
            if (resp.isSuccessful) {
                val dtos = resp.body() ?: emptyList()
                val ofertas = dtos.map { it.toModel() }
                Result.success(ofertas)
            } else {
                Result.failure(Exception("Erro ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** CRUD Ofertas conforme Seção 1.3 */
    suspend fun criarOferta(request: OfertaCreateRequest): ApiResult<OfertaResponse> = withContext(Dispatchers.IO) {
        try {
            // Convertemos o request de criação para o formato do body esperado
            val apiRequest = OfertaRequest(request.nome, "ATIVO", request.data, null)
            val resp = api.criarOferta(apiRequest)
            if (resp.isSuccessful && resp.body() != null) {
                ApiResult.Success(resp.body()!!)
            } else {
                ApiResult.Error(Exception("Erro ao criar oferta"))
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    suspend fun uploadImagem(id: String, file: File): ApiResult<OfertaResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = file.asRequestBody("image/*".toMediaType())
            val part = MultipartBody.Part.createFormData("imagem", file.name, requestBody)
            val resp = api.uploadImagemOferta(id, part)
            if (resp.isSuccessful && resp.body() != null) {
                ApiResult.Success(resp.body()!!)
            } else {
                ApiResult.Error(Exception("Erro no upload"))
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    suspend fun deleteOferta(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = api.deletarOferta(id)
            if (resp.isSuccessful) {
                dao.deleteById(id)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro ao deletar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Mapeamentos ───────────────────────────────────────────────────────────────

fun OfertaResponse.toModel() = Oferta().apply {
    ofertaId = id
    mercadoId = mercado?.id ?: this@toModel.mercadoId
    nome = this@toModel.nome
    status = this@toModel.status
    dataValidade = data ?: dataFim
    imagemOferta = imagem ?: this@toModel.imagemOferta
    nomeSupermercado = mercado?.nome
    enderecoSupermercado = mercado?.endereco
    imagemLogo = mercado?.imagemLogo
    latitude = mercado?.latitude
    longitude = mercado?.longitude
}

fun OfertaResponse.toEntity() = OfertaEntity(
    id = id,
    nome = nome,
    status = status,
    data = data ?: dataFim,
    imagemOferta = imagem ?: imagemOferta,
    mercadoId = mercadoId ?: mercado?.id ?: "",
    mercadoNome = mercado?.nome ?: "",
    mercadoCnpj = mercado?.cnpj,
    mercadoEndereco = mercado?.endereco,
    mercadoImagemLogo = mercado?.imagemLogo,
    mercadoEmail = mercado?.email ?: "",
    mercadoLatitude = mercado?.latitude,
    mercadoLongitude = mercado?.longitude
)

fun OfertaEntity.toResponse() = OfertaResponse(
    id = id,
    mercadoId = mercadoId,
    nome = nome,
    status = status ?: "ATIVO",
    data = data ?: "",
    dataFim = data ?: "",
    imagemOferta = imagemOferta,
    imagem = imagemOferta,
    mercado = MercadoResumoDto(mercadoId, mercadoNome, mercadoCnpj, mercadoEndereco, mercadoImagemLogo, mercadoEmail, mercadoLatitude, mercadoLongitude)
)
