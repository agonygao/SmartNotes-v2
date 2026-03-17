package com.smartnotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartnotes.data.local.dao.DocumentDao
import com.smartnotes.data.local.dao.NoteDao
import com.smartnotes.data.local.dao.WordBookDao
import com.smartnotes.data.local.dao.WordDao
import com.smartnotes.data.local.entity.DocumentEntity
import com.smartnotes.data.local.entity.NoteEntity
import com.smartnotes.data.local.entity.WordBookEntity
import com.smartnotes.data.local.entity.WordEntity

@Database(
    entities = [
        NoteEntity::class,
        WordBookEntity::class,
        WordEntity::class,
        DocumentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun wordBookDao(): WordBookDao
    abstract fun wordDao(): WordDao
    abstract fun documentDao(): DocumentDao
}
