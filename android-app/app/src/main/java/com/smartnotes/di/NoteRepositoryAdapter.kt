package com.smartnotes.di

import com.smartnotes.data.local.entity.NoteEntity
import com.smartnotes.data.repository.NoteRepository as DataNoteRepository
import com.smartnotes.ui.viewmodel.*
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryAdapter @Inject constructor(
    private val noteRepository: DataNoteRepository
) : com.smartnotes.ui.viewmodel.NoteRepository {

    override suspend fun getNotes(
        page: Int,
        pageSize: Int,
        type: String?,
        search: String?
    ): Result<List<Note>> {
        return try {
            noteRepository.refreshNotes()
            val allNotes = if (type != null) {
                noteRepository.getNotesByType(type)
            } else {
                noteRepository.observeAllNotes().first()
            }
            val filtered = if (search.isNullOrBlank()) {
                allNotes
            } else {
                allNotes.filter {
                    it.title.contains(search, ignoreCase = true) ||
                        it.content.contains(search, ignoreCase = true)
                }
            }
            val sorted = filtered.sortedWith(
                compareByDescending<NoteEntity> { it.isPinned }
                    .thenByDescending { it.updatedAt }
            )
            val paged = sorted.drop(page * pageSize).take(pageSize + 1)
            Result.success(paged.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNoteById(id: Long): Result<Note> {
        val entity = noteRepository.getNoteById(id)
            ?: return Result.failure(Exception("Note not found"))
        return Result.success(entity.toDomain())
    }

    override suspend fun createNote(request: CreateNoteRequest): Result<Note> {
        return noteRepository.createNote(
            title = request.title,
            content = request.content,
            type = request.type,
            checklistItems = request.checklistItems?.joinToString("\n") { "${it.id}|${it.text}|${it.isChecked}" },
            reminderTime = request.reminderTime,
            reminderRepeatRule = request.reminderRepeatRule,
            reminderRingtone = request.reminderRingtone,
            isEncrypted = request.isEncrypted,
        ).map { it.toDomain(isPinned = request.isPinned) }
    }

    override suspend fun updateNote(request: UpdateNoteRequest): Result<Note> {
        return noteRepository.updateNote(
            id = request.id,
            title = request.title,
            content = request.content,
            type = request.type,
            checklistItems = request.checklistItems?.joinToString("\n") { "${it.id}|${it.text}|${it.isChecked}" },
            reminderTime = request.reminderTime,
            reminderRepeatRule = request.reminderRepeatRule,
            reminderRingtone = request.reminderRingtone,
            isEncrypted = request.isEncrypted,
        ).map { it.toDomain(isPinned = request.isPinned, isCompleted = request.isCompleted) }
    }

    override suspend fun deleteNote(id: Long): Result<Unit> {
        return noteRepository.deleteNote(id)
    }

    override suspend fun togglePin(id: Long, pinned: Boolean): Result<Unit> {
        return noteRepository.togglePin(id).map { }
    }

    override suspend fun toggleComplete(id: Long, completed: Boolean): Result<Unit> {
        return noteRepository.toggleComplete(id).map { }
    }

    override suspend fun syncNotes(): Result<Unit> {
        return try {
            noteRepository.refreshNotes()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun NoteEntity.toDomain(
    isPinned: Boolean? = null,
    isCompleted: Boolean? = null,
): Note {
    val typeEnum = try {
        NoteType.valueOf(type)
    } catch (_: Exception) {
        NoteType.NORMAL
    }
    val checklistItems = checklistItems?.split("\n")?.mapNotNull { line ->
        val parts = line.split("|")
        if (parts.size >= 2) {
            ChecklistItem(
                id = parts[0],
                text = parts[1],
                isChecked = parts.getOrElse(2) { "false" }.toBoolean(),
            )
        } else null
    } ?: emptyList()

    val parsedCreatedAt = parseDateTime(createdAt)
    val parsedUpdatedAt = parseDateTime(updatedAt)

    return Note(
        id = id,
        clientId = clientId ?: "",
        title = title,
        content = content,
        type = typeEnum,
        checklistItems = checklistItems,
        reminderTime = reminderTime?.let { parseDateTime(it) },
        reminderRepeatRule = reminderRepeatRule,
        reminderRingtone = reminderRingtone,
        isCompleted = isCompleted ?: this.isCompleted,
        isPinned = isPinned ?: this.isPinned,
        isEncrypted = isEncrypted,
        version = version,
        createdAt = parsedCreatedAt,
        updatedAt = parsedUpdatedAt,
    )
}

private fun parseDateTime(dateTimeStr: String): LocalDateTime {
    return try {
        LocalDateTime.parse(dateTimeStr)
    } catch (_: Exception) {
        LocalDateTime.now()
    }
}
