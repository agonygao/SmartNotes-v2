package com.smartnotes.data.repository

import com.smartnotes.data.api.ApiService
import com.smartnotes.data.api.NoteRequest
import com.smartnotes.data.api.NoteResponse
import com.smartnotes.data.local.dao.NoteDao
import com.smartnotes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val apiService: ApiService,
    private val noteDao: NoteDao
) {

    fun observeAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }

    suspend fun refreshNotes() {
        try {
            var page = 0
            var hasMore = true
            while (hasMore) {
                val response = apiService.getNotes(page = page, size = 50)
                if (response.isSuccess && response.data != null) {
                    val pageData = response.data
                    val entities = pageData.content.map { it.toEntity() }
                    noteDao.insertNotes(entities)
                    hasMore = !pageData.last
                    page++
                } else {
                    break
                }
            }
        } catch (_: Exception) {
            // Silently fail - use local data as fallback
        }
    }

    suspend fun getNoteById(id: Long): NoteEntity? {
        return noteDao.getNoteById(id)
    }

    suspend fun getNotesByType(type: String): List<NoteEntity> {
        return noteDao.getNotesByType(type)
    }

    suspend fun createNote(
        title: String,
        content: String,
        type: String = "NORMAL",
        checklistItems: String? = null,
        reminderTime: String? = null,
        reminderRepeatRule: String? = null,
        reminderRingtone: String? = null,
        isEncrypted: Boolean = false
    ): Result<NoteEntity> {
        val clientId = UUID.randomUUID().toString()
        return try {
            val response = apiService.createNote(
                NoteRequest(
                    title = title,
                    content = content,
                    type = type,
                    checklistItems = checklistItems,
                    reminderTime = reminderTime,
                    reminderRepeatRule = reminderRepeatRule,
                    reminderRingtone = reminderRingtone,
                    isEncrypted = isEncrypted
                )
            )
            if (response.isSuccess && response.data != null) {
                val entity = response.data.toEntity().copy(isSynced = true)
                val localId = noteDao.insertNote(entity)
                Result.success(entity.copy(id = localId))
            } else {
                // Save locally if server fails
                val entity = NoteEntity(
                    title = title,
                    content = content,
                    type = type,
                    checklistItems = checklistItems,
                    reminderTime = reminderTime,
                    reminderRepeatRule = reminderRepeatRule,
                    reminderRingtone = reminderRingtone,
                    isEncrypted = isEncrypted,
                    isSynced = false,
                    clientId = clientId
                )
                val localId = noteDao.insertNote(entity)
                Result.success(entity.copy(id = localId))
            }
        } catch (e: Exception) {
            // Save locally if network fails
            val entity = NoteEntity(
                title = title,
                content = content,
                type = type,
                checklistItems = checklistItems,
                reminderTime = reminderTime,
                reminderRepeatRule = reminderRepeatRule,
                reminderRingtone = reminderRingtone,
                isEncrypted = isEncrypted,
                isSynced = false,
                clientId = clientId
            )
            val localId = noteDao.insertNote(entity)
            Result.success(entity.copy(id = localId))
        }
    }

    suspend fun updateNote(
        id: Long,
        title: String?,
        content: String?,
        type: String? = null,
        checklistItems: String? = null,
        reminderTime: String? = null,
        reminderRepeatRule: String? = null,
        reminderRingtone: String? = null,
        isEncrypted: Boolean? = null
    ): Result<NoteEntity> {
        val existing = noteDao.getNoteById(id) ?: return Result.failure(Exception("Note not found"))
        return try {
            val request = NoteRequest(
                title = title ?: existing.title,
                content = content ?: existing.content,
                type = type ?: existing.type,
                checklistItems = checklistItems,
                reminderTime = reminderTime,
                reminderRepeatRule = reminderRepeatRule,
                reminderRingtone = reminderRingtone,
                isEncrypted = isEncrypted
            )
            val response = if (existing.remoteId != null) {
                apiService.updateNote(existing.remoteId, request)
            } else {
                null
            }
            if (response != null && response.isSuccess && response.data != null) {
                val updated = response.data.toEntity().copy(id = existing.id, isSynced = true)
                noteDao.updateNote(updated)
                Result.success(updated)
            } else {
                val updated = existing.copy(
                    title = title ?: existing.title,
                    content = content ?: existing.content,
                    type = type ?: existing.type,
                    checklistItems = checklistItems ?: existing.checklistItems,
                    reminderTime = reminderTime ?: existing.reminderTime,
                    reminderRepeatRule = reminderRepeatRule ?: existing.reminderRepeatRule,
                    reminderRingtone = reminderRingtone ?: existing.reminderRingtone,
                    isEncrypted = isEncrypted ?: existing.isEncrypted,
                    isSynced = false
                )
                noteDao.updateNote(updated)
                Result.success(updated)
            }
        } catch (e: Exception) {
            val updated = existing.copy(
                title = title ?: existing.title,
                content = content ?: existing.content,
                type = type ?: existing.type,
                checklistItems = checklistItems ?: existing.checklistItems,
                reminderTime = reminderTime ?: existing.reminderTime,
                reminderRepeatRule = reminderRepeatRule ?: existing.reminderRepeatRule,
                reminderRingtone = reminderRingtone ?: existing.reminderRingtone,
                isEncrypted = isEncrypted ?: existing.isEncrypted,
                isSynced = false
            )
            noteDao.updateNote(updated)
            Result.success(updated)
        }
    }

    suspend fun deleteNote(id: Long): Result<Unit> {
        val existing = noteDao.getNoteById(id) ?: return Result.failure(Exception("Note not found"))
        return try {
            if (existing.remoteId != null) {
                apiService.deleteNote(existing.remoteId)
            }
            noteDao.softDeleteNote(id)
            Result.success(Unit)
        } catch (e: Exception) {
            noteDao.softDeleteNote(id)
            Result.success(Unit)
        }
    }

    suspend fun togglePin(id: Long): Result<NoteEntity> {
        val existing = noteDao.getNoteById(id) ?: return Result.failure(Exception("Note not found"))
        return try {
            if (existing.remoteId != null) {
                val response = apiService.togglePin(existing.remoteId)
                if (response.isSuccess && response.data != null) {
                    val updated = response.data.toEntity().copy(id = existing.id, isSynced = true)
                    noteDao.updateNote(updated)
                    return Result.success(updated)
                }
            }
            val updated = existing.copy(isPinned = !existing.isPinned, isSynced = false)
            noteDao.updateNote(updated)
            Result.success(updated)
        } catch (e: Exception) {
            val updated = existing.copy(isPinned = !existing.isPinned, isSynced = false)
            noteDao.updateNote(updated)
            Result.success(updated)
        }
    }

    suspend fun toggleComplete(id: Long): Result<NoteEntity> {
        val existing = noteDao.getNoteById(id) ?: return Result.failure(Exception("Note not found"))
        return try {
            if (existing.remoteId != null) {
                val response = apiService.toggleComplete(existing.remoteId)
                if (response.isSuccess && response.data != null) {
                    val updated = response.data.toEntity().copy(id = existing.id, isSynced = true)
                    noteDao.updateNote(updated)
                    return Result.success(updated)
                }
            }
            val updated = existing.copy(isCompleted = !existing.isCompleted, isSynced = false)
            noteDao.updateNote(updated)
            Result.success(updated)
        } catch (e: Exception) {
            val updated = existing.copy(isCompleted = !existing.isCompleted, isSynced = false)
            noteDao.updateNote(updated)
            Result.success(updated)
        }
    }

    suspend fun getUnsyncedNotes(): List<NoteEntity> {
        return noteDao.getUnsyncedNotes()
    }

    suspend fun syncNote(note: NoteEntity): Result<Unit> {
        return try {
            if (note.remoteId != null) {
                val response = apiService.updateNote(
                    note.remoteId,
                    NoteRequest(
                        title = note.title,
                        content = note.content,
                        type = note.type,
                        checklistItems = note.checklistItems,
                        reminderTime = note.reminderTime,
                        reminderRepeatRule = note.reminderRepeatRule,
                        reminderRingtone = note.reminderRingtone,
                        isEncrypted = note.isEncrypted
                    )
                )
                if (response.isSuccess) {
                    noteDao.markAsSynced(note.id)
                }
            } else {
                val response = apiService.createNote(
                    NoteRequest(
                        title = note.title,
                        content = note.content,
                        type = note.type,
                        checklistItems = note.checklistItems,
                        reminderTime = note.reminderTime,
                        reminderRepeatRule = note.reminderRepeatRule,
                        reminderRingtone = note.reminderRingtone,
                        isEncrypted = note.isEncrypted
                    )
                )
                if (response.isSuccess && response.data != null) {
                    val synced = note.copy(
                        remoteId = response.data.id,
                        isSynced = true,
                        version = response.data.version
                    )
                    noteDao.updateNote(synced)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun NoteResponse.toEntity(): NoteEntity {
        return NoteEntity(
            remoteId = id,
            userId = userId,
            title = title,
            content = content,
            type = type,
            checklistItems = checklistItems,
            reminderTime = reminderTime,
            reminderRepeatRule = reminderRepeatRule,
            reminderRingtone = reminderRingtone,
            isCompleted = isCompleted,
            isPinned = isPinned,
            isEncrypted = isEncrypted,
            clientId = clientId,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
