package com.smartnotes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartnotes.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words WHERE bookId = :bookId AND isDeleted = 0 ORDER BY sortOrder ASC")
    fun getWordsByBookId(bookId: Long): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE bookId = :bookId AND isDeleted = 0 ORDER BY sortOrder ASC")
    suspend fun getWordsByBookIdList(bookId: Long): List<WordEntity>

    @Query("SELECT * FROM words WHERE id = :id AND isDeleted = 0")
    suspend fun getWordById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE remoteId = :remoteId AND isDeleted = 0")
    suspend fun getWordByRemoteId(remoteId: Long): WordEntity?

    @Query("SELECT * FROM words WHERE bookId = :bookId AND word LIKE '%' || :keyword || '%' AND isDeleted = 0")
    suspend fun searchWordsByBookId(bookId: Long, keyword: String): List<WordEntity>

    @Query("SELECT * FROM words WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedWords(): List<WordEntity>

    @Query("SELECT * FROM words WHERE isDeleted = 1")
    suspend fun getDeletedWords(): List<WordEntity>

    @Query("SELECT * FROM words WHERE clientId = :clientId")
    suspend fun getWordByClientId(clientId: String): WordEntity?

    @Query("SELECT * FROM words WHERE bookId = :bookId AND isDeleted = 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWords(bookId: Long, limit: Int): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId AND isDeleted = 0")
    suspend fun getWordCountByBookId(bookId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Update
    suspend fun updateWord(word: WordEntity)

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Query("UPDATE words SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteWord(id: Long)

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun hardDeleteWord(id: Long)

    @Query("DELETE FROM words WHERE isDeleted = 1")
    suspend fun clearDeletedWords()

    @Query("UPDATE words SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("DELETE FROM words WHERE bookId = :bookId")
    suspend fun deleteWordsByBookId(bookId: Long)
}
