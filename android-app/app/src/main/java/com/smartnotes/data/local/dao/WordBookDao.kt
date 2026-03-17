package com.smartnotes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartnotes.data.local.entity.WordBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordBookDao {

    @Query("SELECT * FROM word_books WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllWordBooks(): Flow<List<WordBookEntity>>

    @Query("SELECT * FROM word_books WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllWordBooksList(): List<WordBookEntity>

    @Query("SELECT * FROM word_books WHERE id = :id AND isDeleted = 0")
    suspend fun getWordBookById(id: Long): WordBookEntity?

    @Query("SELECT * FROM word_books WHERE remoteId = :remoteId AND isDeleted = 0")
    suspend fun getWordBookByRemoteId(remoteId: Long): WordBookEntity?

    @Query("SELECT * FROM word_books WHERE isDefault = 1 AND isDeleted = 0")
    suspend fun getDefaultWordBooks(): List<WordBookEntity>

    @Query("SELECT * FROM word_books WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedWordBooks(): List<WordBookEntity>

    @Query("SELECT * FROM word_books WHERE isDeleted = 1")
    suspend fun getDeletedWordBooks(): List<WordBookEntity>

    @Query("SELECT * FROM word_books WHERE clientId = :clientId")
    suspend fun getWordBookByClientId(clientId: String): WordBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordBook(wordBook: WordBookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordBooks(wordBooks: List<WordBookEntity>)

    @Update
    suspend fun updateWordBook(wordBook: WordBookEntity)

    @Delete
    suspend fun deleteWordBook(wordBook: WordBookEntity)

    @Query("UPDATE word_books SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteWordBook(id: Long)

    @Query("DELETE FROM word_books WHERE id = :id")
    suspend fun hardDeleteWordBook(id: Long)

    @Query("DELETE FROM word_books WHERE isDeleted = 1")
    suspend fun clearDeletedWordBooks()

    @Query("UPDATE word_books SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("UPDATE word_books SET wordCount = :count WHERE id = :id")
    suspend fun updateWordCount(id: Long, count: Int)
}
