package com.smartnotes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = WordBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"])
    ]
)
data class WordEntity(
    @PrimaryKey val id: Long = 0,
    val remoteId: Long? = null,
    val bookId: Long = 0,
    val word: String = "",
    val phonetic: String? = null,
    val meaning: String? = null,
    val exampleSentence: String? = null,
    val sortOrder: Int = 0,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val clientId: String? = null,
    val version: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)
