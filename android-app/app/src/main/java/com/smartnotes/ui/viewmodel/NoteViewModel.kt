package com.smartnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotes.ui.components.NoteType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Data models for Notes
// ---------------------------------------------------------------------------
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
)

data class Note(
    val id: Long = 0L,
    val clientId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val type: NoteType = NoteType.NORMAL,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val reminderTime: LocalDateTime? = null,
    val reminderRepeatRule: String? = null,
    val reminderRingtone: String? = null,
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val isEncrypted: Boolean = false,
    val version: Int = 1,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

data class CreateNoteRequest(
    val title: String,
    val content: String,
    val type: String,
    val checklistItems: List<ChecklistItem>? = null,
    val reminderTime: String? = null,
    val reminderRepeatRule: String? = null,
    val reminderRingtone: String? = null,
    val isPinned: Boolean = false,
    val isEncrypted: Boolean = false,
    val clientId: String = UUID.randomUUID().toString(),
    val password: String? = null,
)

data class UpdateNoteRequest(
    val id: Long,
    val title: String,
    val content: String,
    val type: String,
    val checklistItems: List<ChecklistItem>? = null,
    val reminderTime: String? = null,
    val reminderRepeatRule: String? = null,
    val reminderRingtone: String? = null,
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val isEncrypted: Boolean = false,
    val version: Int,
    val clientId: String? = null,
    val password: String? = null,
)

// ---------------------------------------------------------------------------
// Notes UI state
// ---------------------------------------------------------------------------
sealed class NotesUiState {
    data object Loading : NotesUiState()
    data class Success(val notes: List<Note>, val hasMore: Boolean = false) : NotesUiState()
    data class Error(val message: String) : NotesUiState()
}

sealed class NoteEditUiState {
    data object Idle : NoteEditUiState()
    data object Loading : NoteEditUiState()
    data class Loaded(val note: Note) : NoteEditUiState()
    data class Saved(val note: Note) : NoteEditUiState()
    data class Error(val message: String) : NoteEditUiState()
}

// ---------------------------------------------------------------------------
// NoteRepository interface
// ---------------------------------------------------------------------------
interface NoteRepository {
    suspend fun getNotes(
        page: Int,
        pageSize: Int,
        type: String? = null,
        search: String? = null,
    ): Result<List<Note>>

    suspend fun getNoteById(id: Long): Result<Note>
    suspend fun createNote(request: CreateNoteRequest): Result<Note>
    suspend fun updateNote(request: UpdateNoteRequest): Result<Note>
    suspend fun deleteNote(id: Long): Result<Unit>
    suspend fun togglePin(id: Long, pinned: Boolean): Result<Unit>
    suspend fun toggleComplete(id: Long, completed: Boolean): Result<Unit>
    suspend fun syncNotes(): Result<Unit>
}

// ---------------------------------------------------------------------------
// NoteFilter enum
// ---------------------------------------------------------------------------
enum class NoteFilter(val label: String, val apiValue: String?) {
    ALL("All", null),
    NORMAL("Normal", "NORMAL"),
    CHECKLIST("Checklist", "CHECKLIST"),
    REMINDER("Reminder", "REMINDER"),
    SECRET("Secret", "SECRET"),
}

// ---------------------------------------------------------------------------
// NoteViewModel
// ---------------------------------------------------------------------------
@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _notesState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val notesState: StateFlow<NotesUiState> = _notesState.asStateFlow()

    private val _noteEditState = MutableStateFlow<NoteEditUiState>(NoteEditUiState.Idle)
    val noteEditState: StateFlow<NoteEditUiState> = _noteEditState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(NoteFilter.ALL)
    val activeFilter: StateFlow<NoteFilter> = _activeFilter.asStateFlow()

    private val _deleteConfirmNoteId = MutableStateFlow<Long?>(null)
    val deleteConfirmNoteId: StateFlow<Long?> = _deleteConfirmNoteId.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20

    init {
        loadNotes()
    }

    /**
     * Loads notes with the current filter and search query.
     */
    fun loadNotes(append: Boolean = false) {
        viewModelScope.launch {
            if (!append) {
                currentPage = 0
                _notesState.value = NotesUiState.Loading
            }

            val result = noteRepository.getNotes(
                page = currentPage,
                pageSize = pageSize,
                type = _activeFilter.value.apiValue,
                search = _searchQuery.value.ifBlank { null },
            )

            if (result.isSuccess) {
                val newNotes = result.getOrNull() ?: emptyList()
                val existingNotes = if (append && _notesState.value is NotesUiState.Success) {
                    (_notesState.value as NotesUiState.Success).notes
                } else {
                    emptyList()
                }
                val allNotes = (existingNotes + newNotes).sortedWith(
                    compareByDescending<Note> { it.isPinned }
                        .thenByDescending { it.updatedAt }
                )
                _notesState.value = NotesUiState.Success(
                    notes = allNotes,
                    hasMore = newNotes.size == pageSize,
                )
            } else {
                if (!append) {
                    _notesState.value = NotesUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to load notes"
                    )
                }
            }
        }
    }

    /**
     * Refresh notes (pull-to-refresh).
     */
    fun refreshNotes() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadNotes()
            _isRefreshing.value = false
        }
    }

    /**
     * Load more notes (pagination).
     */
    fun loadMore() {
        val state = _notesState.value
        if (state is NotesUiState.Success && state.hasMore) {
            currentPage++
            loadNotes(append = true)
        }
    }

    /**
     * Update the search query and reload notes.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        loadNotes()
    }

    /**
     * Set the active filter tab and reload notes.
     */
    fun setFilter(filter: NoteFilter) {
        _activeFilter.value = filter
        loadNotes()
    }

    /**
     * Load a single note by ID for editing.
     */
    fun loadNoteForEdit(noteId: Long) {
        viewModelScope.launch {
            _noteEditState.value = NoteEditUiState.Loading
            val result = noteRepository.getNoteById(noteId)
            if (result.isSuccess) {
                _noteEditState.value = NoteEditUiState.Loaded(result.getOrNull()!!)
            } else {
                _noteEditState.value = NoteEditUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load note"
                )
            }
        }
    }

    /**
     * Save a note (create or update).
     */
    fun saveNote(
        noteId: Long?,
        title: String,
        content: String,
        type: NoteType,
        checklistItems: List<ChecklistItem> = emptyList(),
        reminderTime: LocalDateTime? = null,
        reminderRepeatRule: String? = null,
        reminderRingtone: String? = null,
        isPinned: Boolean = false,
        isEncrypted: Boolean = false,
        password: String? = null,
    ) {
        if (title.isBlank()) {
            _noteEditState.value = NoteEditUiState.Error("Title must not be empty")
            return
        }

        viewModelScope.launch {
            _noteEditState.value = NoteEditUiState.Loading

            val result = if (noteId != null && noteId > 0) {
                val existingNote = if (_noteEditState.value is NoteEditUiState.Loaded) {
                    (_noteEditState.value as NoteEditUiState.Loaded).note
                } else {
                    noteRepository.getNoteById(noteId).getOrNull()
                }

                val version = existingNote?.version ?: 1
                noteRepository.updateNote(
                    UpdateNoteRequest(
                        id = noteId,
                        title = title,
                        content = content,
                        type = type.name,
                        checklistItems = checklistItems.ifEmpty { null },
                        reminderTime = reminderTime?.toString(),
                        reminderRepeatRule = reminderRepeatRule,
                        reminderRingtone = reminderRingtone,
                        isPinned = isPinned,
                        isEncrypted = isEncrypted,
                        version = version,
                        clientId = existingNote?.clientId,
                        password = password,
                    )
                )
            } else {
                noteRepository.createNote(
                    CreateNoteRequest(
                        title = title,
                        content = content,
                        type = type.name,
                        checklistItems = checklistItems.ifEmpty { null },
                        reminderTime = reminderTime?.toString(),
                        reminderRepeatRule = reminderRepeatRule,
                        reminderRingtone = reminderRingtone,
                        isPinned = isPinned,
                        isEncrypted = isEncrypted,
                        password = password,
                    )
                )
            }

            if (result.isSuccess) {
                _noteEditState.value = NoteEditUiState.Saved(result.getOrNull()!!)
            } else {
                _noteEditState.value = NoteEditUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to save note"
                )
            }
        }
    }

    /**
     * Delete a note by ID.
     */
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            val result = noteRepository.deleteNote(noteId)
            if (result.isSuccess) {
                _deleteConfirmNoteId.value = null
                loadNotes()
            } else {
                // Could emit an error state, but for simplicity just reload
                loadNotes()
            }
        }
    }

    /**
     * Toggle the pin status of a note.
     */
    fun togglePin(note: Note) {
        viewModelScope.launch {
            noteRepository.togglePin(note.id, !note.isPinned)
            loadNotes()
        }
    }

    /**
     * Toggle the completion status of a note.
     */
    fun toggleComplete(note: Note) {
        viewModelScope.launch {
            noteRepository.toggleComplete(note.id, !note.isCompleted)
            loadNotes()
        }
    }

    /**
     * Show delete confirmation dialog for a note.
     */
    fun showDeleteConfirm(noteId: Long?) {
        _deleteConfirmNoteId.value = noteId
    }

    /**
     * Dismiss delete confirmation dialog.
     */
    fun dismissDeleteConfirm() {
        _deleteConfirmNoteId.value = null
    }

    /**
     * Reset note edit state to idle.
     */
    fun resetNoteEditState() {
        _noteEditState.value = NoteEditUiState.Idle
    }

    /**
     * Sync notes with the server.
     */
    fun syncNotes() {
        viewModelScope.launch {
            noteRepository.syncNotes()
            loadNotes()
        }
    }
}
