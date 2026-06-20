package com.example.wofertas.data.repository

import android.content.Context
import android.net.Uri
import com.example.wofertas.data.local.AppDatabase
import com.example.wofertas.data.local.entities.EncarteEntity
import com.example.wofertas.network.ApiClient
import com.example.wofertas.network.EncarteDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class EncarteRepository(private val context: Context) {

    private val api get() = ApiClient.authService(context)
    private val dao = AppDatabase.getInstance(context).encarteDao()
    private val analytics = AnalyticsRepository(context)

    fun getEncartesFlow(mercadoId: String): Flow<List<EncarteEntity>> =
        dao.getByMercadoFlow(mercadoId)

    suspend fun fetchEncartes(mercadoId: String): Result<List<EncarteDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getEncartesByMercado(mercadoId)
                if (response.isSuccessful) {
                    val lista = response.body() ?: emptyList()
                    
                    // Analytics: Track visualização do mercado/encartes (fire and forget)
                    analytics.trackEvent(mercadoId, AnalyticsRepository.VISUALIZACAO)
                    
                    dao.clearByMercado(mercadoId)
                    dao.insertAll(lista.map { it.toEntity() })
                    Result.success(lista)
                } else {
                    val cached = dao.getByMercado(mercadoId)
                    Result.success(cached.map { it.toDto() })
                }
            } catch (e: Exception) {
                val cached = dao.getByMercado(mercadoId)
                Result.success(cached.map { it.toDto() })
            }
        }

    /**
     * Upload de encarte PDF via multipart/form-data.
     */
    suspend fun uploadEncarte(
        mercadoId: String,
        titulo: String,
        pdfUri: Uri
    ): Result<EncarteDto> = withContext(Dispatchers.IO) {
        try {
            val tmpFile = File(context.cacheDir, "encarte_upload_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
                FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(Exception("Não foi possível ler o arquivo PDF"))

            val pdfBody = tmpFile.asRequestBody("application/pdf".toMediaTypeOrNull())
            val pdfPart = MultipartBody.Part.createFormData("pdf", tmpFile.name, pdfBody)

            val mercadoIdBody = mercadoId.toRequestBody("text/plain".toMediaTypeOrNull())
            val tituloBody    = titulo.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.uploadEncarte(mercadoIdBody, tituloBody, pdfPart)

            tmpFile.delete()

            if (response.isSuccessful && response.body() != null) {
                val encarte = response.body()!!
                dao.insert(encarte.toEntity())
                Result.success(encarte)
            } else {
                Result.failure(Exception("Erro no upload: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEncarte(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteEncarte(id)
            return@withContext if (response.isSuccessful) {
                dao.deleteById(id)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Mapeamentos ───────────────────────────────────────────────────────────────
fun EncarteDto.toEntity() = EncarteEntity(
    id = id,
    mercadoId = mercadoId,
    titulo = titulo,
    urlPdf = urlPdf,
    dataCriacao = dataCriacao
)

fun EncarteEntity.toDto() = EncarteDto(
    id = id,
    mercadoId = mercadoId,
    titulo = titulo,
    urlPdf = urlPdf,
    dataCriacao = dataCriacao ?: ""
)
