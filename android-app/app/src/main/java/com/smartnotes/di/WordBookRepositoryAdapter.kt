package com.smartnotes.di

import com.smartnotes.data.local.entity.WordBookEntity
import com.smartnotes.data.local.entity.WordEntity
import com.smartnotes.data.repository.WordBookRepository as DataWordBookRepository
import com.smartnotes.ui.viewmodel.*
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordBookRepositoryAdapter @Inject constructor(
    private val wordBookRepository: DataWordBookRepository
) : com.smartnotes.ui.viewmodel.WordBookRepository {

    override suspend fun getWordBooks(): Result<List<WordBook>> {
        return try {
            wordBookRepository.refreshWordBooks()
            val entities = wordBookRepository.observeAllWordBooks().first()
            Result.success(entities.filter { !it.isDeleted }.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWordBookById(id: Long): Result<WordBook> {
        val entity = wordBookRepository.getWordBookById(id)
            ?: return Result.failure(Exception("Word book not found"))
        return Result.success(entity.toDomain())
    }

    override suspend fun createWordBook(name: String, description: String?): Result<WordBook> {
        return wordBookRepository.createWordBook(name = name, description = description).map { it.toDomain() }
    }

    override suspend fun updateWordBook(id: Long, name: String, description: String?): Result<WordBook> {
        return wordBookRepository.updateWordBook(id = id, name = name, description = description).map { it.toDomain() }
    }

    override suspend fun deleteWordBook(id: Long): Result<Unit> {
        return wordBookRepository.deleteWordBook(id)
    }

    override suspend fun getWordsForBook(bookId: Long): Result<List<Word>> {
        return try {
            val bookEntity = wordBookRepository.getWordBookById(bookId)
            if (bookEntity != null && bookEntity.remoteId != null) {
                wordBookRepository.refreshWords(bookEntity.remoteId, bookId)
            }
            val entities = wordBookRepository.getWordsByBookId(bookId)
            Result.success(entities.filter { !it.isDeleted }.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addWord(bookId: Long, word: Word): Result<Word> {
        val bookEntity = wordBookRepository.getWordBookById(bookId)
        return wordBookRepository.addWord(
            remoteBookId = bookEntity?.remoteId ?: 0,
            localBookId = bookId,
            word = word.word,
            phonetic = word.phonetic,
            meaning = word.meaning,
            exampleSentence = word.exampleSentence,
        ).map { it.toDomain() }
    }

    override suspend fun updateWord(word: Word): Result<Word> {
        val bookEntity = wordBookRepository.getWordBookById(word.bookId)
        return wordBookRepository.updateWord(
            remoteBookId = bookEntity?.remoteId ?: 0,
            wordId = word.id,
            word = word.word,
            phonetic = word.phonetic,
            meaning = word.meaning,
            exampleSentence = word.exampleSentence,
        ).map { it.toDomain() }
    }

    override suspend fun deleteWord(wordId: Long): Result<Unit> {
        return wordBookRepository.deleteWord(remoteBookId = 0, wordId = wordId)
    }

    override suspend fun getWordsForReview(bookId: Long, limit: Int): Result<List<Word>> {
        return try {
            val entities = wordBookRepository.getRandomWordsForReview(bookId, limit)
            Result.success(entities.filter { !it.isDeleted }.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitReviewResults(bookId: Long, results: List<ReviewResult>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun submitDictationResults(bookId: Long, results: List<DictationResult>): Result<Unit> {
        return Result.success(Unit)
    }
}

private fun WordBookEntity.toDomain(): WordBook {
    val typeEnum = when (type) {
        "CET4" -> WordBookType.CET4
        "CET6" -> WordBookType.CET6
        else -> WordBookType.CUSTOM
    }
    return WordBook(
        id = id,
        clientId = clientId ?: "",
        name = name,
        description = description,
        type = typeEnum,
        wordCount = wordCount,
        isDefault = isDefault,
        version = version,
        createdAt = parseDateTime(createdAt),
        updatedAt = parseDateTime(updatedAt),
    )
}

private fun WordEntity.toDomain(): Word {
    return Word(
        id = id,
        bookId = bookId,
        word = word,
        phonetic = phonetic,
        meaning = meaning,
        exampleSentence = exampleSentence,
        sortOrder = sortOrder,
        version = version,
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
