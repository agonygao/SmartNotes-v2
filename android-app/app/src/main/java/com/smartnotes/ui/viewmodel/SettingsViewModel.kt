package com.smartnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.smartnotes.core.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Theme preference
// ---------------------------------------------------------------------------
enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System"),
}

// ---------------------------------------------------------------------------
// Settings UI state
// ---------------------------------------------------------------------------
data class SettingsState(
    val backendUrl: String = "http://10.0.2.2:8080",
    val autoSync: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = LocaleHelper.ENGLISH,
    val appVersion: String = "1.0.0",
    val isSaving: Boolean = false,
    val isSyncing: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isClearingCache: Boolean = false,
    val lastSyncTime: String? = null,
    val message: String? = null,
    val isError: Boolean = false,
    val syncError: String? = null,
)

// ---------------------------------------------------------------------------
// Preferences manager interface
// ---------------------------------------------------------------------------
interface PreferencesManager {
    fun getBackendUrl(): String
    fun saveBackendUrl(url: String)
    fun getAutoSync(): Boolean
    fun saveAutoSync(enabled: Boolean)
    fun getThemeMode(): ThemeMode
    fun saveThemeMode(theme: ThemeMode)
    fun getLastSyncTime(): String?
    fun saveLastSyncTime(time: String?)
    fun getLanguage(): String
    fun saveLanguage(language: String)
    fun clearAll()
}

// ---------------------------------------------------------------------------
// SettingsViewModel
// ---------------------------------------------------------------------------
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _settingsState = MutableStateFlow(
        SettingsState(
            backendUrl = preferencesManager.getBackendUrl(),
            autoSync = preferencesManager.getAutoSync(),
            themeMode = preferencesManager.getThemeMode(),
            language = preferencesManager.getLanguage(),
            lastSyncTime = preferencesManager.getLastSyncTime(),
        )
    )
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    /**
     * Update the backend URL and persist it.
     */
    fun updateBackendUrl(url: String) {
        _settingsState.value = _settingsState.value.copy(backendUrl = url)
    }

    /**
     * Save the backend URL to persistent storage.
     */
    fun saveBackendUrl() {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isSaving = true)
            val url = _settingsState.value.backendUrl.trim()
            if (url.isBlank()) {
                showMessage("Backend URL cannot be empty", isError = true)
            } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                showMessage("URL must start with http:// or https://", isError = true)
            } else {
                preferencesManager.saveBackendUrl(url)
                showMessage("Backend URL saved successfully", isError = false)
            }
            _settingsState.value = _settingsState.value.copy(isSaving = false)
        }
    }

    /**
     * Toggle auto-sync preference.
     */
    fun toggleAutoSync(enabled: Boolean) {
        preferencesManager.saveAutoSync(enabled)
        _settingsState.value = _settingsState.value.copy(autoSync = enabled)
    }

    /**
     * Set the theme mode and persist it.
     */
    fun setThemeMode(theme: ThemeMode) {
        preferencesManager.saveThemeMode(theme)
        _settingsState.value = _settingsState.value.copy(themeMode = theme)
    }

    fun setLanguage(language: String) {
        preferencesManager.saveLanguage(language)
        _settingsState.value = _settingsState.value.copy(language = language)
        showMessage("Language changed. Restart app to take full effect.", isError = false)
    }

    /**
     * Update the last sync time display.
     */
    fun updateLastSyncTime(time: String?) {
        preferencesManager.saveLastSyncTime(time)
        _settingsState.value = _settingsState.value.copy(lastSyncTime = time, syncError = null)
    }

    /**
     * Perform a sync operation with error feedback.
     * [syncAction] should call the appropriate repository refresh methods.
     */
    fun syncNow(syncAction: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isSyncing = true, syncError = null)
            val result = syncAction()
            if (result.isSuccess) {
                val now = LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                )
                preferencesManager.saveLastSyncTime(now)
                _settingsState.value = _settingsState.value.copy(
                    isSyncing = false,
                    lastSyncTime = now,
                    syncError = null,
                )
            } else {
                _settingsState.value = _settingsState.value.copy(
                    isSyncing = false,
                    syncError = result.exceptionOrNull()?.message ?: "Sync failed",
                )
            }
        }
    }

    /**
     * Perform logout - delegates to the caller (e.g., AuthViewModel).
     * Returns true to signal the UI layer to handle logout navigation.
     */
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isLoggingOut = true)
            // Clear preferences (but keep backend URL)
            preferencesManager.saveAutoSync(false)
            preferencesManager.clearAll()
            _settingsState.value = _settingsState.value.copy(
                isLoggingOut = false,
                autoSync = false,
                lastSyncTime = null,
            )
            onComplete()
        }
    }

    /**
     * Clear all cached data.
     */
    fun clearCache(onComplete: () -> Unit) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isClearingCache = true)
            // Clear sync time
            preferencesManager.saveLastSyncTime(null)
            _settingsState.value = _settingsState.value.copy(
                isClearingCache = false,
                lastSyncTime = null,
            )
            showMessage("Cache cleared successfully", isError = false)
            onComplete()
        }
    }

    /**
     * Show a transient message in the settings screen.
     */
    private fun showMessage(message: String, isError: Boolean) {
        _settingsState.value = _settingsState.value.copy(
            message = message,
            isError = isError,
        )
    }

    /**
     * Clear the transient message.
     */
    fun clearMessage() {
        _settingsState.value = _settingsState.value.copy(message = null)
    }
}
