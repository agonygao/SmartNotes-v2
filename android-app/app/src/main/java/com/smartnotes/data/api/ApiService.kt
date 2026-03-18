package com.smartnotes.data.api

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // ==================== Auth ====================

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<UserDTO>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @GET("api/auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") authHeader: String?,
        @Query("refreshToken") refreshToken: String?
    ): ApiResponse<LoginResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): ApiResponse<UserDTO>

    // ==================== Notes ====================

    @GET("api/notes")
    suspend fun getNotes(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("type") type: String? = null
    ): ApiResponse<PageResponse<NoteResponse>>

    @GET("api/notes/{id}")
    suspend fun getNote(@Path("id") id: Long): ApiResponse<NoteResponse>

    @POST("api/notes")
    suspend fun createNote(@Body request: NoteRequest): ApiResponse<NoteResponse>

    @PUT("api/notes/{id}")
    suspend fun updateNote(@Path("id") id: Long, @Body request: NoteRequest): ApiResponse<NoteResponse>

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") id: Long): ApiResponse<Void>

    @PATCH("api/notes/{id}/pin")
    suspend fun togglePin(@Path("id") id: Long): ApiResponse<NoteResponse>

    @PATCH("api/notes/{id}/complete")
    suspend fun toggleComplete(@Path("id") id: Long): ApiResponse<NoteResponse>

    // ==================== Word Books ====================

    @GET("api/wordbooks")
    suspend fun getWordBooks(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<PageResponse<WordBookResponse>>

    @GET("api/wordbooks/defaults")
    suspend fun getDefaultWordBooks(): ApiResponse<List<WordBookResponse>>

    @GET("api/wordbooks/{id}")
    suspend fun getWordBook(@Path("id") id: Long): ApiResponse<WordBookResponse>

    @POST("api/wordbooks")
    suspend fun createWordBook(@Body request: WordBookRequest): ApiResponse<WordBookResponse>

    @PUT("api/wordbooks/{id}")
    suspend fun updateWordBook(@Path("id") id: Long, @Body request: WordBookRequest): ApiResponse<WordBookResponse>

    @DELETE("api/wordbooks/{id}")
    suspend fun deleteWordBook(@Path("id") id: Long): ApiResponse<Void>

    // ==================== Words ====================

    @GET("api/wordbooks/{bookId}/words")
    suspend fun getWords(
        @Path("bookId") bookId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<PageResponse<WordResponse>>

    @GET("api/wordbooks/{bookId}/words/search")
    suspend fun searchWords(
        @Path("bookId") bookId: Long,
        @Query("keyword") keyword: String
    ): ApiResponse<List<WordResponse>>

    @GET("api/wordbooks/{bookId}/words/{wordId}")
    suspend fun getWord(
        @Path("bookId") bookId: Long,
        @Path("wordId") wordId: Long
    ): ApiResponse<WordResponse>

    @POST("api/wordbooks/{bookId}/words")
    suspend fun addWord(
        @Path("bookId") bookId: Long,
        @Query("word") word: String,
        @Query("phonetic") phonetic: String? = null,
        @Query("meaning") meaning: String? = null,
        @Query("exampleSentence") exampleSentence: String? = null
    ): ApiResponse<WordResponse>

    @PUT("api/wordbooks/{bookId}/words/{wordId}")
    suspend fun updateWord(
        @Path("bookId") bookId: Long,
        @Path("wordId") wordId: Long,
        @Query("word") word: String? = null,
        @Query("phonetic") phonetic: String? = null,
        @Query("meaning") meaning: String? = null,
        @Query("exampleSentence") exampleSentence: String? = null
    ): ApiResponse<WordResponse>

    @DELETE("api/wordbooks/{bookId}/words/{wordId}")
    suspend fun deleteWord(
        @Path("bookId") bookId: Long,
        @Path("wordId") wordId: Long
    ): ApiResponse<Void>

    // ==================== Documents ====================

    @GET("api/documents")
    suspend fun getDocuments(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<PageResponse<DocumentUploadResponse>>

    @GET("api/documents/{id}")
    suspend fun getDocument(@Path("id") id: Long): ApiResponse<DocumentUploadResponse>

    @Multipart
    @POST("api/documents/upload")
    suspend fun uploadDocument(@Part file: MultipartBody.Part): ApiResponse<DocumentUploadResponse>

    @DELETE("api/documents/{id}")
    suspend fun deleteDocument(@Path("id") id: Long): ApiResponse<Void>

    // ==================== Sync ====================

    @GET("api/sync/pull")
    suspend fun syncPull(
        @Query("cursor") cursor: Long = 0,
        @Query("pageSize") pageSize: Int = 100
    ): ApiResponse<SyncPullResponse>

    @POST("api/sync/push")
    suspend fun syncPush(@Body changes: List<SyncPushRequest>): ApiResponse<SyncPushResponse>

    @GET("api/sync/status")
    suspend fun syncStatus(): ApiResponse<Map<String, Any>>

    @GET("api/sync/conflicts")
    suspend fun syncConflicts(): ApiResponse<SyncConflictResponse>

    // ==================== Word Review ====================

    @POST("api/review/words")
    suspend fun getReviewWords(@Body request: WordReviewRequest): ApiResponse<WordReviewResponse>

    @POST("api/review/result")
    suspend fun submitReviewResult(@Body request: WordReviewResultRequest): ApiResponse<Void>

    @GET("api/review/wrong-words")
    suspend fun getWrongWords(): ApiResponse<List<WordReviewItem>>

    @GET("api/review/dictation/stats")
    suspend fun getDictationStats(): ApiResponse<Map<String, Any>>
}

// ==================== DTOs ====================

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)

data class UserDTO(
    val id: Long,
    val username: String,
    val email: String?,
    val role: String,
    val status: String,
    val createdAt: String
)

data class NoteRequest(
    val title: String? = null,
    val content: String? = null,
    val type: String = "NORMAL",
    val checklistItems: String? = null,
    val reminderTime: String? = null,
    val reminderRepeatRule: String? = null,
    val reminderRingtone: String? = null,
    val isEncrypted: Boolean? = null
)

data class NoteResponse(
    val id: Long,
    val userId: Long,
    val title: String,
    val content: String,
    val type: String,
    val checklistItems: String?,
    val reminderTime: String?,
    val reminderRepeatRule: String?,
    val reminderRingtone: String?,
    val isCompleted: Boolean,
    val isPinned: Boolean,
    val isEncrypted: Boolean,
    val clientId: String?,
    val version: Int,
    val createdAt: String,
    val updatedAt: String
)

data class WordBookRequest(
    val name: String,
    val description: String? = null,
    val type: String? = null
)

data class WordBookResponse(
    val id: Long,
    val userId: Long,
    val name: String,
    val description: String?,
    val type: String?,
    val wordCount: Int,
    val isDefault: Boolean,
    val clientId: String?,
    val version: Int,
    val createdAt: String,
    val updatedAt: String
)

data class WordResponse(
    val id: Long,
    val bookId: Long,
    val word: String,
    val phonetic: String?,
    val meaning: String?,
    val exampleSentence: String?,
    val sortOrder: Int,
    val clientId: String?,
    val version: Int,
    val createdAt: String,
    val updatedAt: String
)

data class DocumentUploadResponse(
    val id: Long,
    val filename: String,
    val originalFilename: String,
    val fileType: String,
    val fileSize: Long,
    val previewAvailable: Boolean,
    val createdAt: String
)

data class SyncPushRequest(
    val clientId: String,
    val entityType: String,
    val entityId: Long,
    val action: String,
    val data: String,
    val version: Int
)

data class SyncPushResponse(
    val results: List<SyncResultEntry>
)

data class SyncResultEntry(
    val entityType: String,
    val clientId: String,
    val entityId: Long,
    val serverVersion: Int,
    val status: String
)

data class SyncPullResponse(
    val cursor: Long,
    val hasMore: Boolean,
    val changes: List<SyncChangeEntry>
)

data class SyncChangeEntry(
    val entityType: String,
    val entityId: Long,
    val action: String,
    val data: String,
    val version: Int,
    val serverTimestamp: Long
)

data class SyncConflictResponse(
    val conflicts: List<ConflictEntry>
)

data class ConflictEntry(
    val entityType: String,
    val entityId: Long,
    val clientId: String?,
    val localVersion: Int,
    val serverVersion: Int,
    val localData: String,
    val serverData: String,
    val createdAt: String
)

data class WordReviewRequest(
    val bookId: Long? = null,
    val wordIds: List<Long>? = null,
    val mode: String = "REVIEW",
    val pageSize: Int = 20
)

data class WordReviewResponse(
    val words: List<WordReviewItem>,
    val hasMore: Boolean
)

data class WordReviewItem(
    val wordId: Long,
    val word: String,
    val phonetic: String?,
    val meaning: String?,
    val exampleSentence: String?,
    val masteryLevel: Int
)

data class WordReviewResultRequest(
    val wordId: Long,
    val isCorrect: Boolean,
    val mode: String
)
