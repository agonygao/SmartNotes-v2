package com.smartnotes.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        private val KEY_SYNC_INTERVAL = stringPreferencesKey("sync_interval")
        private val KEY_REVIEW_MODE = stringPreferencesKey("review_mode")
        private val KEY_REVIEW_PAGE_SIZE = stringPreferencesKey("review_page_size")

        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8080/"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
        const val SYNC_INTERVAL_15MIN = "15"
        const val SYNC_INTERVAL_30MIN = "30"
        const val SYNC_INTERVAL_1HOUR = "60"
        const val SYNC_INTERVAL_MANUAL = "manual"
        const val REVIEW_MODE_REVIEW = "REVIEW"
        const val REVIEW_MODE_DICTATION = "DICTATION"
        const val DEFAULT_REVIEW_PAGE_SIZE = "20"
    }

    val backendUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_BACKEND_URL] ?: DEFAULT_BACKEND_URL
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME] ?: THEME_SYSTEM
    }

    val syncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SYNC_ENABLED] ?: true
    }

    val syncIntervalFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SYNC_INTERVAL] ?: SYNC_INTERVAL_MANUAL
    }

    val reviewModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_REVIEW_MODE] ?: REVIEW_MODE_REVIEW
    }

    val reviewPageSizeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_REVIEW_PAGE_SIZE] ?: DEFAULT_REVIEW_PAGE_SIZE
    }

    suspend fun getBackendUrl(): String {
        return context.dataStore.data.first()[KEY_BACKEND_URL] ?: DEFAULT_BACKEND_URL
    }

    suspend fun getTheme(): String {
        return context.dataStore.data.first()[KEY_THEME] ?: THEME_SYSTEM
    }

    suspend fun isSyncEnabled(): Boolean {
        return context.dataStore.data.first()[KEY_SYNC_ENABLED] ?: true
    }

    suspend fun getSyncInterval(): String {
        return context.dataStore.data.first()[KEY_SYNC_INTERVAL] ?: SYNC_INTERVAL_MANUAL
    }

    suspend fun getReviewMode(): String {
        return context.dataStore.data.first()[KEY_REVIEW_MODE] ?: REVIEW_MODE_REVIEW
    }

    suspend fun getReviewPageSize(): Int {
        val value = context.dataStore.data.first()[KEY_REVIEW_PAGE_SIZE] ?: DEFAULT_REVIEW_PAGE_SIZE
        return value.toIntOrNull() ?: DEFAULT_REVIEW_PAGE_SIZE.toInt()
    }

    suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { preferences ->
            val normalizedUrl = if (url.endsWith("/")) url else "$url/"
            preferences[KEY_BACKEND_URL] = normalizedUrl
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setSyncInterval(interval: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SYNC_INTERVAL] = interval
        }
    }

    suspend fun setReviewMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REVIEW_MODE] = mode
        }
    }

    suspend fun setReviewPageSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REVIEW_PAGE_SIZE] = size.toString()
        }
    }
}
