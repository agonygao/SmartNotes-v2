package com.smartnotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: Long = 0,
    val remoteId: Long? = null,
    val userId: Long? = null,
    val title: String = "",
    val content: String = "",
    val type: String = "NORMAL",
    val checklistItems: String? = null,
    val reminderTime: String? = null,
    val reminderRepeatRule: String? = null,
    val reminderRingtone: String? = null,
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val isEncrypted: Boolean = false,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val clientId: String? = null,
    val version: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)
