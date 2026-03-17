package com.smartnotes.di

import com.smartnotes.data.repository.AuthRepository
import com.smartnotes.data.repository.DocumentRepository
import com.smartnotes.data.repository.NoteRepository
import com.smartnotes.data.repository.WordBookRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(authRepository: AuthRepository): AuthRepository {
        return authRepository
    }

    @Provides
    @Singleton
    fun provideNoteRepository(noteRepository: NoteRepository): NoteRepository {
        return noteRepository
    }

    @Provides
    @Singleton
    fun provideWordBookRepository(wordBookRepository: WordBookRepository): WordBookRepository {
        return wordBookRepository
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(documentRepository: DocumentRepository): DocumentRepository {
        return documentRepository
    }
}
