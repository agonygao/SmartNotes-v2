package com.smartnotes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartnotes.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllDocumentsList(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id AND isDeleted = 0")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE remoteId = :remoteId AND isDeleted = 0")
    suspend fun getDocumentByRemoteId(remoteId: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE isDeleted = 1")
    suspend fun getDeletedDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE clientId = :clientId")
    suspend fun getDocumentByClientId(clientId: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteDocument(id: Long)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun hardDeleteDocument(id: Long)

    @Query("DELETE FROM documents WHERE isDeleted = 1")
    suspend fun clearDeletedDocuments()

    @Query("UPDATE documents SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}
