package com.smartnotes.di

import com.smartnotes.data.local.entity.DocumentEntity
import com.smartnotes.data.repository.DocumentRepository as DataDocumentRepository
import com.smartnotes.ui.viewmodel.*
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryAdapter @Inject constructor(
    private val documentRepository: DataDocumentRepository
) : com.smartnotes.ui.viewmodel.DocumentRepository {

    override suspend fun getDocuments(): Result<List<Document>> {
        return try {
            documentRepository.refreshDocuments()
            val entities = documentRepository.observeAllDocuments().first()
            Result.success(entities.filter { !it.isDeleted }.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDocumentById(id: Long): Result<Document> {
        val entity = documentRepository.getDocumentById(id)
            ?: return Result.failure(Exception("Document not found"))
        return Result.success(entity.toDomain())
    }

    override suspend fun uploadDocument(
        filename: String,
        mimeType: String,
        fileSize: Long,
        onProgress: (Float) -> Unit
    ): Result<Document> {
        onProgress(0.5f)
        val entity = DocumentEntity(
            originalFilename = filename,
            fileType = filename.substringAfterLast('.', "file"),
            fileSize = fileSize,
            isSynced = false,
            clientId = UUID.randomUUID().toString(),
        )
        return Result.success(entity.toDomain())
    }

    override suspend fun deleteDocument(id: Long): Result<Unit> {
        return documentRepository.deleteDocument(id)
    }

    override suspend fun downloadDocument(id: Long): Result<String> {
        val entity = documentRepository.getDocumentById(id)
            ?: return Result.failure(Exception("Document not found"))
        return Result.success("[Document content for: ${entity.originalFilename}]")
    }
}

private fun DocumentEntity.toDomain(): Document {
    return Document(
        id = id,
        clientId = clientId ?: "",
        userId = 0,
        filename = filename,
        originalFilename = originalFilename,
        fileType = fileType,
        fileSize = fileSize,
        filePath = localUri ?: "",
        mimeType = null,
        previewAvailable = previewAvailable,
        version = 1,
        createdAt = parseDateTime(createdAt),
        updatedAt = parseDateTime(updatedAt),
    )
}

private fun parseDateTime(dateTimeStr: String): LocalDateTime {
    return try {
        LocalDateTime.parse(dateTimeStr)
    } catch (_: Exception) {
        LocalDateTime.now()
    }
}
