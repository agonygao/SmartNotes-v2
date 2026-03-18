package com.smartnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotes.data.api.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Data models for Word Books
// ---------------------------------------------------------------------------
data class WordBook(
    val id: Long = 0L,
    val clientId: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val type: WordBookType = WordBookType.CUSTOM,
    val wordCount: Int = 0,
    val isDefault: Boolean = false,
    val version: Int = 1,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

data class Word(
    val id: Long = 0L,
    val bookId: Long = 0L,
    val word: String,
    val phonetic: String? = null,
    val meaning: String? = null,
    val exampleSentence: String? = null,
    val sortOrder: Int = 0,
    val version: Int = 1,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

data class WordProgress(
    val wordId: Long,
    val masteryLevel: Int = 0,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastReviewedAt: LocalDateTime? = null,
    val nextReviewAt: LocalDateTime? = null,
)

enum class WordBookType(val label: String) {
    CET4("CET-4"),
    CET6("CET-6"),
    CUSTOM("Custom"),
}

// ---------------------------------------------------------------------------
// UI states for word books
// ---------------------------------------------------------------------------
sealed class WordBooksUiState {
    data object Loading : WordBooksUiState()
    data class Success(val wordBooks: List<WordBook>, val hasMore: Boolean = false) : WordBooksUiState()
    data class Error(val message: String) : WordBooksUiState()
}

sealed class WordsUiState {
    data object Loading : WordsUiState()
    data class Success(val words: List<Word>) : WordsUiState()
    data class Error(val message: String) : WordsUiState()
}

// ---------------------------------------------------------------------------
// Review session state
// ---------------------------------------------------------------------------
data class ReviewSession(
    val bookId: Long,
    val bookName: String,
    val words: List<Word>,
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val results: List<ReviewResult> = emptyList(),
    val isFinished: Boolean = false,
)

data class ReviewResult(
    val word: Word,
    val isCorrect: Boolean,
)

// ---------------------------------------------------------------------------
// Dictation session state
// ---------------------------------------------------------------------------
data class DictationSession(
    val bookId: Long,
    val bookName: String,
    val words: List<Word>,
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val currentAnswer: String = "",
    val showResult: Boolean = false,
    val isCorrect: Boolean = false,
    val isFinished: Boolean = false,
    val results: List<DictationResult> = emptyList(),
)

data class DictationResult(
    val word: Word,
    val userAnswer: String,
    val isCorrect: Boolean,
)

// ---------------------------------------------------------------------------
// Repository interface
// ---------------------------------------------------------------------------
interface WordBookRepository {
    suspend fun getWordBooks(page: Int = 0, size: Int = 20): Result<List<WordBook>>
    suspend fun getWordBookById(id: Long): Result<WordBook>
    suspend fun createWordBook(name: String, description: String?): Result<WordBook>
    suspend fun updateWordBook(id: Long, name: String, description: String?): Result<WordBook>
    suspend fun deleteWordBook(id: Long): Result<Unit>
    suspend fun getWordsForBook(bookId: Long): Result<List<Word>>
    suspend fun addWord(bookId: Long, word: Word): Result<Word>
    suspend fun updateWord(word: Word): Result<Word>
    suspend fun deleteWord(wordId: Long): Result<Unit>
    suspend fun getWordsForReview(bookId: Long, limit: Int = 20): Result<List<Word>>
    suspend fun submitReviewResults(bookId: Long, results: List<ReviewResult>): Result<Unit>
    suspend fun submitDictationResults(bookId: Long, results: List<DictationResult>): Result<Unit>
}

// ---------------------------------------------------------------------------
// WordBookViewModel
// ---------------------------------------------------------------------------
@HiltViewModel
class WordBookViewModel @Inject constructor(
    private val wordBookRepository: WordBookRepository,
) : ViewModel() {

    private val _wordBooksState = MutableStateFlow<WordBooksUiState>(WordBooksUiState.Loading)
    val wordBooksState: StateFlow<WordBooksUiState> = _wordBooksState.asStateFlow()

    private val _wordsState = MutableStateFlow<WordsUiState>(WordsUiState.Loading)
    val wordsState: StateFlow<WordsUiState> = _wordsState.asStateFlow()

    private val _currentWordBook = MutableStateFlow<WordBook?>(null)
    val currentWordBook: StateFlow<WordBook?> = _currentWordBook.asStateFlow()

    private val _reviewSession = MutableStateFlow<ReviewSession?>(null)
    val reviewSession: StateFlow<ReviewSession?> = _reviewSession.asStateFlow()

    private val _dictationSession = MutableStateFlow<DictationSession?>(null)
    val dictationSession: StateFlow<DictationSession?> = _dictationSession.asStateFlow()

    private val _isOperationLoading = MutableStateFlow(false)
    val isOperationLoading: StateFlow<Boolean> = _isOperationLoading.asStateFlow()

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var _currentPage = 0
    private var _hasMoreBooks = false
    private val _pageSize = 20

    init {
        loadWordBooks()
    }

    // -------------------------------------------------------------------------
    // Word Book operations
    // -------------------------------------------------------------------------

    fun loadWordBooks() {
        viewModelScope.launch {
            _wordBooksState.value = WordBooksUiState.Loading
            _syncStatus.value = SyncStatus.SYNCING
            _currentPage = 0
            val result = wordBookRepository.getWordBooks(page = _currentPage, size = _pageSize)
            if (result.isSuccess) {
                val books = result.getOrNull()!!
                _hasMoreBooks = books.size >= _pageSize
                _wordBooksState.value = WordBooksUiState.Success(books, _hasMoreBooks)
                _syncStatus.value = SyncStatus.SYNCED
            } else {
                _syncStatus.value = SyncStatus.ERROR
                _wordBooksState.value = WordBooksUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load word books"
                )
            }
        }
    }

    fun loadMoreWordBooks() {
        if (!_hasMoreBooks) return
        val currentState = _wordBooksState.value
        if (currentState !is WordBooksUiState.Success) return
        viewModelScope.launch {
            _currentPage++
            val result = wordBookRepository.getWordBooks(page = _currentPage, size = _pageSize)
            if (result.isSuccess) {
                val newBooks = result.getOrNull()!!
                _hasMoreBooks = newBooks.size >= _pageSize
                _wordBooksState.value = WordBooksUiState.Success(
                    currentState.wordBooks + newBooks, _hasMoreBooks
                )
            }
        }
    }

    fun loadWordBookDetail(bookId: Long) {
        viewModelScope.launch {
            val bookResult = wordBookRepository.getWordBookById(bookId)
            if (bookResult.isSuccess) {
                _currentWordBook.value = bookResult.getOrNull()
            }

            _wordsState.value = WordsUiState.Loading
            val wordsResult = wordBookRepository.getWordsForBook(bookId)
            if (wordsResult.isSuccess) {
                _wordsState.value = WordsUiState.Success(wordsResult.getOrNull()!!)
            } else {
                _wordsState.value = WordsUiState.Error(
                    wordsResult.exceptionOrNull()?.message ?: "Failed to load words"
                )
            }
        }
    }

    fun createWordBook(name: String, description: String?) {
        if (name.isBlank()) {
            _operationError.value = "Name must not be empty"
            return
        }
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = wordBookRepository.createWordBook(name, description)
            if (result.isSuccess) {
                _operationError.value = null
                loadWordBooks()
            } else {
                _operationError.value = result.exceptionOrNull()?.message ?: "Failed to create word book"
            }
            _isOperationLoading.value = false
        }
    }

    fun deleteWordBook(bookId: Long) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = wordBookRepository.deleteWordBook(bookId)
            if (result.isSuccess) {
                _operationError.value = null
                loadWordBooks()
            } else {
                _operationError.value = result.exceptionOrNull()?.message ?: "Failed to delete word book"
            }
            _isOperationLoading.value = false
        }
    }

    // -------------------------------------------------------------------------
    // Word operations
    // -------------------------------------------------------------------------

    fun addWord(word: Word) {
        if (word.word.isBlank()) {
            _operationError.value = "Word must not be empty"
            return
        }
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = wordBookRepository.addWord(word.bookId, word)
            if (result.isSuccess) {
                _operationError.value = null
                _currentWordBook.value?.id?.let { loadWordBookDetail(it) }
            } else {
                _operationError.value = result.exceptionOrNull()?.message ?: "Failed to add word"
            }
            _isOperationLoading.value = false
        }
    }

    fun deleteWord(wordId: Long) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = wordBookRepository.deleteWord(wordId)
            if (result.isSuccess) {
                _operationError.value = null
                _currentWordBook.value?.id?.let { loadWordBookDetail(it) }
            } else {
                _operationError.value = result.exceptionOrNull()?.message ?: "Failed to delete word"
            }
            _isOperationLoading.value = false
        }
    }

    // -------------------------------------------------------------------------
    // Review session management
    // -------------------------------------------------------------------------

    fun startReviewSession(bookId: Long) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val bookResult = wordBookRepository.getWordBookById(bookId)
            val wordsResult = wordBookRepository.getWordsForReview(bookId)

            if (bookResult.isSuccess && wordsResult.isSuccess) {
                val book = bookResult.getOrNull()!!
                val words = wordsResult.getOrNull()!!.shuffled()
                _reviewSession.value = ReviewSession(
                    bookId = bookId,
                    bookName = book.name,
                    words = words,
                )
            } else {
                _operationError.value = "Failed to start review session"
            }
            _isOperationLoading.value = false
        }
    }

    fun markReviewWord(correct: Boolean) {
        val session = _reviewSession.value ?: return
        val currentWord = session.words.getOrNull(session.currentIndex) ?: return

        val newResult = ReviewResult(word = currentWord, isCorrect = correct)
        val newResults = session.results + newResult
        val nextIndex = session.currentIndex + 1
        val isFinished = nextIndex >= session.words.size

        _reviewSession.value = session.copy(
            currentIndex = nextIndex,
            correctCount = session.correctCount + (if (correct) 1 else 0),
            wrongCount = session.wrongCount + (if (!correct) 1 else 0),
            results = newResults,
            isFinished = isFinished,
        )
    }

    fun submitReviewResults() {
        val session = _reviewSession.value ?: return
        viewModelScope.launch {
            wordBookRepository.submitReviewResults(session.bookId, session.results)
        }
    }

    fun resetReviewSession() {
        _reviewSession.value = null
    }

    // -------------------------------------------------------------------------
    // Dictation session management
    // -------------------------------------------------------------------------

    fun startDictationSession(bookId: Long) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val bookResult = wordBookRepository.getWordBookById(bookId)
            val wordsResult = wordBookRepository.getWordsForReview(bookId)

            if (bookResult.isSuccess && wordsResult.isSuccess) {
                val book = bookResult.getOrNull()!!
                val words = wordsResult.getOrNull()!!.shuffled()
                _dictationSession.value = DictationSession(
                    bookId = bookId,
                    bookName = book.name,
                    words = words,
                )
            } else {
                _operationError.value = "Failed to start dictation session"
            }
            _isOperationLoading.value = false
        }
    }

    fun updateDictationAnswer(answer: String) {
        _dictationSession.value?.let { session ->
            _dictationSession.value = session.copy(currentAnswer = answer)
        }
    }

    fun submitDictationAnswer() {
        val session = _dictationSession.value ?: return
        val currentWord = session.words.getOrNull(session.currentIndex) ?: return

        val isCorrect = session.currentAnswer.trim().equals(currentWord.word, ignoreCase = true)
        val newResult = DictationResult(
            word = currentWord,
            userAnswer = session.currentAnswer,
            isCorrect = isCorrect,
        )
        val newResults = session.results + newResult

        _dictationSession.value = session.copy(
            showResult = true,
            isCorrect = isCorrect,
            results = newResults,
        )
    }

    fun nextDictationWord() {
        val session = _dictationSession.value ?: return
        val nextIndex = session.currentIndex + 1
        val isFinished = nextIndex >= session.words.size

        _dictationSession.value = session.copy(
            currentIndex = nextIndex,
            correctCount = session.correctCount + (if (session.isCorrect) 1 else 0),
            wrongCount = session.wrongCount + (if (!session.isCorrect) 1 else 0),
            currentAnswer = "",
            showResult = false,
            isCorrect = false,
            isFinished = isFinished,
        )
    }

    fun submitDictationResults() {
        val session = _dictationSession.value ?: return
        viewModelScope.launch {
            wordBookRepository.submitDictationResults(session.bookId, session.results)
        }
    }

    fun resetDictationSession() {
        _dictationSession.value = null
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    fun clearOperationError() {
        _operationError.value = null
    }

    fun clearCurrentBook() {
        _currentWordBook.value = null
        _wordsState.value = WordsUiState.Loading
    }
}
