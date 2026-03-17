package com.smartnotes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartnotes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNotesList(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE remoteId = :remoteId AND isDeleted = 0")
    suspend fun getNoteByRemoteId(remoteId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE type = :type AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getNotesByType(type: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isPinned = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    suspend fun getPinnedNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isCompleted = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    suspend fun getCompletedNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isDeleted = 1")
    suspend fun getDeletedNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE clientId = :clientId")
    suspend fun getNoteByClientId(clientId: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteNote(id: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDeleteNote(id: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun clearDeletedNotes()

    @Query("UPDATE notes SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}
