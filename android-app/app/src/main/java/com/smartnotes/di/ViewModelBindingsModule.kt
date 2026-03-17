package com.smartnotes.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelBindingsModule {

    @Provides
    @Singleton
    fun provideAuthRepository(adapter: AuthRepositoryAdapter): com.smartnotes.ui.viewmodel.AuthRepository {
        return adapter
    }

    @Provides
    @Singleton
    fun provideTokenManager(adapter: TokenManagerAdapter): com.smartnotes.ui.viewmodel.TokenManager {
        return adapter
    }

    @Provides
    @Singleton
    fun provideNoteRepository(adapter: NoteRepositoryAdapter): com.smartnotes.ui.viewmodel.NoteRepository {
        return adapter
    }

    @Provides
    @Singleton
    fun provideWordBookRepository(adapter: WordBookRepositoryAdapter): com.smartnotes.ui.viewmodel.WordBookRepository {
        return adapter
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(adapter: DocumentRepositoryAdapter): com.smartnotes.ui.viewmodel.DocumentRepository {
        return adapter
    }

    @Provides
    @Singleton
    fun providePreferencesManager(adapter: PreferencesManagerAdapter): com.smartnotes.ui.viewmodel.PreferencesManager {
        return adapter
    }

    @Provides
    @Singleton
    fun provideSyncRepository(adapter: SyncRepositoryAdapter): com.smartnotes.ui.viewmodel.SyncRepository {
        return adapter
    }
}
