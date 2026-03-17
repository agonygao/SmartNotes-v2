package com.smartnotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: Long = 0,
    val remoteId: Long? = null,
    val userId: Long? = null,
    val filename: String = "",
    val originalFilename: String = "",
    val fileType: String = "",
    val fileSize: Long = 0,
    val previewAvailable: Boolean = false,
    val localUri: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val clientId: String? = null,
    val version: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)
