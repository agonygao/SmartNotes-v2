package com.smartnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Data models for Documents
// ---------------------------------------------------------------------------
data class Document(
    val id: Long = 0L,
    val clientId: String = UUID.randomUUID().toString(),
    val userId: Long = 0L,
    val filename: String,
    val originalFilename: String,
    val fileType: String,
    val fileSize: Long = 0L,
    val filePath: String,
    val mimeType: String? = null,
    val previewAvailable: Boolean = false,
    val version: Int = 1,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

// ---------------------------------------------------------------------------
// UI states for documents
// ---------------------------------------------------------------------------
sealed class DocumentsUiState {
    data object Loading : DocumentsUiState()
    data class Success(val documents: List<Document>) : DocumentsUiState()
    data class Error(val message: String) : DocumentsUiState()
}

sealed class DocumentUploadUiState {
    data object Idle : DocumentUploadUiState()
    data object Uploading : DocumentUploadUiState()
    data class Progress(val progress: Float) : DocumentUploadUiState()
    data class Success(val document: Document) : DocumentUploadUiState()
    data class Error(val message: String) : DocumentUploadUiState()
}

sealed class DocumentPreviewUiState {
    data object Loading : DocumentPreviewUiState()
    data class Loaded(val document: Document, val content: String?) : DocumentPreviewUiState()
    data class Error(val message: String) : DocumentPreviewUiState()
}

// ---------------------------------------------------------------------------
// Repository interface
// ---------------------------------------------------------------------------
interface DocumentRepository {
    suspend fun getDocuments(): Result<List<Document>>
    suspend fun getDocumentById(id: Long): Result<Document>
    suspend fun uploadDocument(
        filename: String,
        mimeType: String,
        fileSize: Long,
        onProgress: (Float) -> Unit,
    ): Result<Document>
    suspend fun deleteDocument(id: Long): Result<Unit>
    suspend fun downloadDocument(id: Long): Result<String>
}

// ---------------------------------------------------------------------------
// DocumentViewModel
// ---------------------------------------------------------------------------
@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    private val _documentsState = MutableStateFlow<DocumentsUiState>(DocumentsUiState.Loading)
    val documentsState: StateFlow<DocumentsUiState> = _documentsState.asStateFlow()

    private val _uploadState = MutableStateFlow<DocumentUploadUiState>(DocumentUploadUiState.Idle)
    val uploadState: StateFlow<DocumentUploadUiState> = _uploadState.asStateFlow()

    private val _previewState = MutableStateFlow<DocumentPreviewUiState>(DocumentPreviewUiState.Loading)
    val previewState: StateFlow<DocumentPreviewUiState> = _previewState.asStateFlow()

    private val _deleteConfirmDocId = MutableStateFlow<Long?>(null)
    val deleteConfirmDocId: StateFlow<Long?> = _deleteConfirmDocId.asStateFlow()

    init {
        loadDocuments()
    }

    /**
     * Load all documents for the current user.
     */
    fun loadDocuments() {
        viewModelScope.launch {
            _documentsState.value = DocumentsUiState.Loading
            val result = documentRepository.getDocuments()
            if (result.isSuccess) {
                _documentsState.value = DocumentsUiState.Success(result.getOrNull()!!)
            } else {
                _documentsState.value = DocumentsUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load documents"
                )
            }
        }
    }

    /**
     * Upload a new document with progress tracking.
     */
    fun uploadDocument(
        filename: String,
        mimeType: String,
        fileSize: Long,
    ) {
        viewModelScope.launch {
            _uploadState.value = DocumentUploadUiState.Uploading
            val result = documentRepository.uploadDocument(filename, mimeType, fileSize) { progress ->
                _uploadState.value = DocumentUploadUiState.Progress(progress)
            }
            if (result.isSuccess) {
                _uploadState.value = DocumentUploadUiState.Success(result.getOrNull()!!)
                loadDocuments()
            } else {
                _uploadState.value = DocumentUploadUiState.Error(
                    result.exceptionOrNull()?.message ?: "Upload failed"
                )
            }
        }
    }

    /**
     * Load a document for preview.
     */
    fun loadDocumentPreview(docId: Long) {
        viewModelScope.launch {
            _previewState.value = DocumentPreviewUiState.Loading
            val docResult = documentRepository.getDocumentById(docId)
            if (docResult.isFailure) {
                _previewState.value = DocumentPreviewUiState.Error(
                    docResult.exceptionOrNull()?.message ?: "Failed to load document"
                )
                return@launch
            }
            val document = docResult.getOrNull()!!

            // Attempt to download content for text-based files
            val content: String? = if (document.fileType.lowercase() in listOf("txt", "md")) {
                val downloadResult = documentRepository.downloadDocument(docId)
                if (downloadResult.isSuccess) downloadResult.getOrNull() else null
            } else {
                null
            }

            _previewState.value = DocumentPreviewUiState.Loaded(document, content)
        }
    }

    /**
     * Delete a document by ID.
     */
    fun deleteDocument(docId: Long) {
        viewModelScope.launch {
            val result = documentRepository.deleteDocument(docId)
            if (result.isSuccess) {
                _deleteConfirmDocId.value = null
                loadDocuments()
            }
        }
    }

    /**
     * Show delete confirmation dialog.
     */
    fun showDeleteConfirm(docId: Long?) {
        _deleteConfirmDocId.value = docId
    }

    /**
     * Dismiss delete confirmation dialog.
     */
    fun dismissDeleteConfirm() {
        _deleteConfirmDocId.value = null
    }

    /**
     * Reset upload state to idle.
     */
    fun resetUploadState() {
        _uploadState.value = DocumentUploadUiState.Idle
    }

    /**
     * Reset preview state.
     */
    fun resetPreviewState() {
        _previewState.value = DocumentPreviewUiState.Loading
    }
}
