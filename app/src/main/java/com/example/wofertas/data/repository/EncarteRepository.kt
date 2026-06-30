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

    suspend fun uploadEncarte(
        mercadoId: String,
        titulo: String,
        pdfUri: Uri
    ): Result<EncarteDto> = withContext(Dispatchers.IO) {
        var uploadFile: File? = null
        try {
            uploadFile = copyPdfToCache(pdfUri, "encarte_upload")
                ?: return@withContext Result.failure(Exception("Nao foi possivel ler o arquivo PDF"))

            val mercadoIdBody = mercadoId.toRequestBody("text/plain".toMediaTypeOrNull())
            val tituloBody = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = api.uploadEncarte(mercadoIdBody, tituloBody, uploadFile.toPdfPart())

            if (response.isSuccessful && response.body() != null) {
                val encarte = response.body()!!
                dao.insert(encarte.toEntity())
                Result.success(encarte)
            } else {
                Result.failure(Exception("Erro no upload: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            uploadFile?.delete()
        }
    }

    suspend fun updateEncarte(
        id: String,
        titulo: String,
        pdfUri: Uri? = null
    ): Result<EncarteDto> = withContext(Dispatchers.IO) {
        var uploadFile: File? = null
        try {
            val tituloBody = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
            val pdfPart = pdfUri?.let {
                uploadFile = copyPdfToCache(it, "encarte_update")
                    ?: return@withContext Result.failure(Exception("Nao foi possivel ler o novo PDF"))
                uploadFile?.toPdfPart()
            }

            val response = api.atualizarEncarte(id, tituloBody, pdfPart)
            if (response.isSuccessful && response.body() != null) {
                val encarte = response.body()!!
                dao.insert(encarte.toEntity())
                Result.success(encarte)
            } else {
                Result.failure(Exception("Erro ao atualizar: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            uploadFile?.delete()
        }
    }

    suspend fun deleteEncarte(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteEncarte(id)
            if (response.isSuccessful) {
                dao.deleteById(id)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyPdfToCache(uri: Uri, prefix: String): File? {
        val tmpFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
        } ?: return null
        return tmpFile
    }

    private fun File.toPdfPart(): MultipartBody.Part {
        val pdfBody = asRequestBody("application/pdf".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("pdf", name, pdfBody)
    }
}

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
