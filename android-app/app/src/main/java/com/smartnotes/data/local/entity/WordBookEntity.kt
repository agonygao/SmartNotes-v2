package com.smartnotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_books")
data class WordBookEntity(
    @PrimaryKey val id: Long = 0,
    val remoteId: Long? = null,
    val userId: Long? = null,
    val name: String = "",
    val description: String? = null,
    val type: String? = null,
    val wordCount: Int = 0,
    val isDefault: Boolean = false,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val clientId: String? = null,
    val version: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)
