package com.smartnotes.data.repository

import android.util.Log
import com.smartnotes.data.api.ApiService
import com.smartnotes.data.api.DocumentUploadResponse
import com.smartnotes.data.local.dao.DocumentDao
import com.smartnotes.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val apiService: ApiService,
    private val documentDao: DocumentDao
) {

    fun observeAllDocuments(): Flow<List<DocumentEntity>> {
        return documentDao.getAllDocuments()
    }

    suspend fun refreshDocuments(): Result<Unit> {
        return try {
            var page = 0
            var hasMore = true
            while (hasMore) {
                val response = apiService.getDocuments(page = page, size = 50)
                if (response.isSuccess && response.data != null) {
                    val pageData = response.data
                    val entities = pageData.content.map { it.toEntity() }
                    documentDao.insertDocuments(entities)
                    hasMore = !pageData.last
                    page++
                } else {
                    Log.w(TAG, "refreshDocuments: non-success response at page $page: ${response.message}")
                    break
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "refreshDocuments failed", e)
            Result.failure(e)
        }
    }

    suspend fun getDocumentById(id: Long): DocumentEntity? {
        return documentDao.getDocumentById(id)
    }

    suspend fun uploadDocument(
        file: File,
        fileName: String
    ): Result<DocumentEntity> {
        val clientId = UUID.randomUUID().toString()
        return try {
            val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", fileName, requestBody)
            val response = apiService.uploadDocument(multipartBody)
            if (response.isSuccess && response.data != null) {
                val entity = response.data.toEntity(localUri = file.absolutePath).copy(
                    isSynced = true
                )
                val localId = documentDao.insertDocument(entity)
                Result.success(entity.copy(id = localId))
            } else {
                val entity = DocumentEntity(
                    originalFilename = fileName,
                    fileType = getFileExtension(fileName),
                    fileSize = file.length(),
                    localUri = file.absolutePath,
                    isSynced = false,
                    clientId = clientId
                )
                val localId = documentDao.insertDocument(entity)
                Result.success(entity.copy(id = localId))
            }
        } catch (e: Exception) {
            val entity = DocumentEntity(
                originalFilename = fileName,
                fileType = getFileExtension(fileName),
                fileSize = file.length(),
                localUri = file.absolutePath,
                isSynced = false,
                clientId = clientId
            )
            val localId = documentDao.insertDocument(entity)
            Result.success(entity.copy(id = localId))
        }
    }

    suspend fun deleteDocument(id: Long): Result<Unit> {
        val existing = documentDao.getDocumentById(id)
            ?: return Result.failure(Exception("Document not found"))
        return try {
            if (existing.remoteId != null) {
                apiService.deleteDocument(existing.remoteId)
            }
            // Delete local file if it exists
            existing.localUri?.let { uri ->
                val file = File(uri)
                if (file.exists()) {
                    file.delete()
                }
            }
            documentDao.softDeleteDocument(id)
            Result.success(Unit)
        } catch (e: Exception) {
            existing.localUri?.let { uri ->
                val file = File(uri)
                if (file.exists()) {
                    file.delete()
                }
            }
            documentDao.softDeleteDocument(id)
            Result.success(Unit)
        }
    }

    suspend fun syncDocument(document: DocumentEntity): Result<Unit> {
        return try {
            if (!document.isSynced && document.localUri != null) {
                val file = File(document.localUri)
                if (file.exists()) {
                    val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                    val multipartBody = MultipartBody.Part.createFormData(
                        "file",
                        document.originalFilename,
                        requestBody
                    )
                    val response = apiService.uploadDocument(multipartBody)
                    if (response.isSuccess && response.data != null) {
                        val synced = document.copy(
                            remoteId = response.data.id,
                            filename = response.data.filename,
                            isSynced = true
                        )
                        documentDao.updateDocument(synced)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "DocumentRepository"
    }

    private fun DocumentUploadResponse.toEntity(localUri: String? = null): DocumentEntity {
        return DocumentEntity(
            remoteId = id,
            filename = filename,
            originalFilename = originalFilename,
            fileType = fileType,
            fileSize = fileSize,
            previewAvailable = previewAvailable,
            localUri = localUri,
            createdAt = createdAt
        )
    }

    private fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0) fileName.substring(lastDot + 1).lowercase() else ""
    }
}
