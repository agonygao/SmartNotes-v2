package com.smartnotes.data.repository

import com.smartnotes.data.api.ApiService
import com.smartnotes.data.api.WordBookRequest
import com.smartnotes.data.api.WordBookResponse
import com.smartnotes.data.api.WordResponse
import com.smartnotes.data.local.dao.WordBookDao
import com.smartnotes.data.local.dao.WordDao
import com.smartnotes.data.local.entity.WordBookEntity
import com.smartnotes.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordBookRepository @Inject constructor(
    private val apiService: ApiService,
    private val wordBookDao: WordBookDao,
    private val wordDao: WordDao
) {

    fun observeAllWordBooks(): Flow<List<WordBookEntity>> {
        return wordBookDao.getAllWordBooks()
    }

    suspend fun refreshWordBooks() {
        try {
            val response = apiService.getWordBooks()
            if (response.isSuccess && response.data != null) {
                val entities = response.data.map { it.toEntity() }
                wordBookDao.insertWordBooks(entities)
            }
        } catch (_: Exception) {
            // Silently fail - use local data as fallback
        }
    }

    suspend fun getDefaultWordBooks(): Result<List<WordBookEntity>> {
        return try {
            val response = apiService.getDefaultWordBooks()
            if (response.isSuccess && response.data != null) {
                val entities = response.data.map { it.toEntity() }
                wordBookDao.insertWordBooks(entities)
                Result.success(entities)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWordBookById(id: Long): WordBookEntity? {
        return wordBookDao.getWordBookById(id)
    }

    suspend fun createWordBook(
        name: String,
        description: String? = null,
        type: String? = null
    ): Result<WordBookEntity> {
        val clientId = UUID.randomUUID().toString()
        return try {
            val response = apiService.createWordBook(
                WordBookRequest(
                    name = name,
                    description = description,
                    type = type
                )
            )
            if (response.isSuccess && response.data != null) {
                val entity = response.data.toEntity().copy(isSynced = true)
                val localId = wordBookDao.insertWordBook(entity)
                Result.success(entity.copy(id = localId))
            } else {
                val entity = WordBookEntity(
                    name = name,
                    description = description,
                    type = type,
                    isSynced = false,
                    clientId = clientId
                )
                val localId = wordBookDao.insertWordBook(entity)
                Result.success(entity.copy(id = localId))
            }
        } catch (e: Exception) {
            val entity = WordBookEntity(
                name = name,
                description = description,
                type = type,
                isSynced = false,
                clientId = clientId
            )
            val localId = wordBookDao.insertWordBook(entity)
            Result.success(entity.copy(id = localId))
        }
    }

    suspend fun updateWordBook(
        id: Long,
        name: String? = null,
        description: String? = null,
        type: String? = null
    ): Result<WordBookEntity> {
        val existing = wordBookDao.getWordBookById(id)
            ?: return Result.failure(Exception("Word book not found"))
        return try {
            val request = WordBookRequest(
                name = name ?: existing.name,
                description = description,
                type = type
            )
            val response = if (existing.remoteId != null) {
                apiService.updateWordBook(existing.remoteId, request)
            } else {
                null
            }
            if (response != null && response.isSuccess && response.data != null) {
                val updated = response.data.toEntity().copy(
                    id = existing.id,
                    isSynced = true
                )
                wordBookDao.updateWordBook(updated)
                Result.success(updated)
            } else {
                val updated = existing.copy(
                    name = name ?: existing.name,
                    description = description ?: existing.description,
                    type = type ?: existing.type,
                    isSynced = false
                )
                wordBookDao.updateWordBook(updated)
                Result.success(updated)
            }
        } catch (e: Exception) {
            val updated = existing.copy(
                name = name ?: existing.name,
                description = description ?: existing.description,
                type = type ?: existing.type,
                isSynced = false
            )
            wordBookDao.updateWordBook(updated)
            Result.success(updated)
        }
    }

    suspend fun deleteWordBook(id: Long): Result<Unit> {
        val existing = wordBookDao.getWordBookById(id)
            ?: return Result.failure(Exception("Word book not found"))
        return try {
            if (existing.remoteId != null) {
                apiService.deleteWordBook(existing.remoteId)
            }
            wordBookDao.softDeleteWordBook(id)
            Result.success(Unit)
        } catch (e: Exception) {
            wordBookDao.softDeleteWordBook(id)
            Result.success(Unit)
        }
    }

    // ==================== Words ====================

    suspend fun refreshWords(bookId: Long, localBookId: Long) {
        try {
            var page = 0
            var hasMore = true
            while (hasMore) {
                val response = apiService.getWords(bookId = bookId, page = page, size = 100)
                if (response.isSuccess && response.data != null) {
                    val pageData = response.data
                    val entities = pageData.content.map { it.toEntity(localBookId) }
                    wordDao.insertWords(entities)
                    hasMore = !pageData.last
                    page++
                } else {
                    break
                }
            }
            val count = wordDao.getWordCountByBookId(localBookId)
            wordBookDao.updateWordCount(localBookId, count)
        } catch (_: Exception) {
            // Silently fail
        }
    }

    suspend fun getWordsByBookId(bookId: Long): List<WordEntity> {
        return wordDao.getWordsByBookIdList(bookId)
    }

    suspend fun searchWords(bookId: Long, keyword: String): List<WordEntity> {
        return wordDao.searchWordsByBookId(bookId, keyword)
    }

    suspend fun addWord(
        remoteBookId: Long,
        localBookId: Long,
        word: String,
        phonetic: String? = null,
        meaning: String? = null,
        exampleSentence: String? = null
    ): Result<WordEntity> {
        val clientId = UUID.randomUUID().toString()
        return try {
            val response = apiService.addWord(
                bookId = remoteBookId,
                word = word,
                phonetic = phonetic,
                meaning = meaning,
                exampleSentence = exampleSentence
            )
            if (response.isSuccess && response.data != null) {
                val entity = response.data.toEntity(localBookId).copy(isSynced = true)
                val localId = wordDao.insertWord(entity)
                Result.success(entity.copy(id = localId))
            } else {
                val entity = WordEntity(
                    bookId = localBookId,
                    word = word,
                    phonetic = phonetic,
                    meaning = meaning,
                    exampleSentence = exampleSentence,
                    isSynced = false,
                    clientId = clientId
                )
                val localId = wordDao.insertWord(entity)
                Result.success(entity.copy(id = localId))
            }
        } catch (e: Exception) {
            val entity = WordEntity(
                bookId = localBookId,
                word = word,
                phonetic = phonetic,
                meaning = meaning,
                exampleSentence = exampleSentence,
                isSynced = false,
                clientId = clientId
            )
            val localId = wordDao.insertWord(entity)
            Result.success(entity.copy(id = localId))
        }
    }

    suspend fun updateWord(
        remoteBookId: Long,
        wordId: Long,
        word: String? = null,
        phonetic: String? = null,
        meaning: String? = null,
        exampleSentence: String? = null
    ): Result<WordEntity> {
        val existing = wordDao.getWordById(wordId)
            ?: return Result.failure(Exception("Word not found"))
        return try {
            if (existing.remoteId != null) {
                val response = apiService.updateWord(
                    bookId = remoteBookId,
                    wordId = existing.remoteId,
                    word = word,
                    phonetic = phonetic,
                    meaning = meaning,
                    exampleSentence = exampleSentence
                )
                if (response.isSuccess && response.data != null) {
                    val updated = response.data.toEntity(existing.bookId).copy(
                        id = existing.id,
                        isSynced = true
                    )
                    wordDao.updateWord(updated)
                    return Result.success(updated)
                }
            }
            val updated = existing.copy(
                word = word ?: existing.word,
                phonetic = phonetic ?: existing.phonetic,
                meaning = meaning ?: existing.meaning,
                exampleSentence = exampleSentence ?: existing.exampleSentence,
                isSynced = false
            )
            wordDao.updateWord(updated)
            Result.success(updated)
        } catch (e: Exception) {
            val updated = existing.copy(
                word = word ?: existing.word,
                phonetic = phonetic ?: existing.phonetic,
                meaning = meaning ?: existing.meaning,
                exampleSentence = exampleSentence ?: existing.exampleSentence,
                isSynced = false
            )
            wordDao.updateWord(updated)
            Result.success(updated)
        }
    }

    suspend fun deleteWord(remoteBookId: Long, wordId: Long): Result<Unit> {
        val existing = wordDao.getWordById(wordId)
            ?: return Result.failure(Exception("Word not found"))
        return try {
            if (existing.remoteId != null) {
                apiService.deleteWord(remoteBookId, existing.remoteId)
            }
            wordDao.softDeleteWord(wordId)
            Result.success(Unit)
        } catch (e: Exception) {
            wordDao.softDeleteWord(wordId)
            Result.success(Unit)
        }
    }

    suspend fun getRandomWordsForReview(bookId: Long, limit: Int): List<WordEntity> {
        return wordDao.getRandomWords(bookId, limit)
    }

    private fun WordBookResponse.toEntity(): WordBookEntity {
        return WordBookEntity(
            remoteId = id,
            userId = userId,
            name = name,
            description = description,
            type = type,
            wordCount = wordCount,
            isDefault = isDefault,
            clientId = clientId,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun WordResponse.toEntity(localBookId: Long): WordEntity {
        return WordEntity(
            remoteId = id,
            bookId = localBookId,
            word = word,
            phonetic = phonetic,
            meaning = meaning,
            exampleSentence = exampleSentence,
            sortOrder = sortOrder,
            clientId = clientId,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
