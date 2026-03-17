package com.smartnotes.di

import com.smartnotes.data.preference.UserPreferences
import com.smartnotes.ui.viewmodel.PreferencesManager
import com.smartnotes.ui.viewmodel.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManagerAdapter @Inject constructor(
    private val userPreferences: UserPreferences
) : PreferencesManager {

    override fun getBackendUrl(): String {
        return kotlinx.coroutines.runBlocking { userPreferences.getBackendUrl() }
    }

    override fun saveBackendUrl(url: String) {
        kotlinx.coroutines.runBlocking { userPreferences.setBackendUrl(url) }
    }

    override fun getAutoSync(): Boolean {
        return kotlinx.coroutines.runBlocking { userPreferences.isSyncEnabled() }
    }

    override fun saveAutoSync(enabled: Boolean) {
        kotlinx.coroutines.runBlocking { userPreferences.setSyncEnabled(enabled) }
    }

    override fun getThemeMode(): ThemeMode {
        return kotlinx.coroutines.runBlocking {
            when (userPreferences.getTheme()) {
                UserPreferences.THEME_LIGHT -> ThemeMode.LIGHT
                UserPreferences.THEME_DARK -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }
    }

    override fun saveThemeMode(theme: ThemeMode) {
        kotlinx.coroutines.runBlocking {
            userPreferences.setTheme(
                when (theme) {
                    ThemeMode.LIGHT -> UserPreferences.THEME_LIGHT
                    ThemeMode.DARK -> UserPreferences.THEME_DARK
                    ThemeMode.SYSTEM -> UserPreferences.THEME_SYSTEM
                }
            )
        }
    }

    override fun getLastSyncTime(): String? {
        // The current UserPreferences doesn't have last sync time storage
        return null
    }

    override fun saveLastSyncTime(time: String?) {
        // No-op: not stored in UserPreferences currently
    }

    override fun clearAll() {
        kotlinx.coroutines.runBlocking {
            userPreferences.setSyncEnabled(false)
        }
    }
}
