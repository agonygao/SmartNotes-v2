package com.smartnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Data models for Sync
// ---------------------------------------------------------------------------
data class SyncStatus(
    val lastSyncedAt: LocalDateTime? = null,
    val isSyncing: Boolean = false,
    val pendingPushCount: Int = 0,
    val pendingPullCount: Int = 0,
    val conflictCount: Int = 0,
    val lastError: String? = null,
)

data class SyncConflict(
    val id: Long,
    val entityType: String,
    val entityId: Long,
    val clientId: String?,
    val localVersion: Int,
    val serverVersion: Int,
    val localData: String?,
    val serverData: String?,
    val resolved: Boolean,
    val createdAt: LocalDateTime,
)

enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_SERVER,
    MERGE,
}

// ---------------------------------------------------------------------------
// Sync UI states
// ---------------------------------------------------------------------------
sealed class SyncUiState {
    data object Idle : SyncUiState()
    data object Syncing : SyncUiState()
    data object Success : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}

// ---------------------------------------------------------------------------
// Repository interface
// ---------------------------------------------------------------------------
interface SyncRepository {
    suspend fun getSyncStatus(): Result<SyncStatus>
    suspend fun pushSync(): Result<SyncStatus>
    suspend fun pullSync(): Result<SyncStatus>
    suspend fun fullSync(): Result<SyncStatus>
    suspend fun getConflicts(): Result<List<SyncConflict>>
    suspend fun resolveConflict(
        conflictId: Long,
        resolution: ConflictResolution,
    ): Result<Unit>
}

// ---------------------------------------------------------------------------
// SyncViewModel
// ---------------------------------------------------------------------------
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts.asStateFlow()

    private val _conflictResolutionState = MutableStateFlow<Result<Unit>?>(null)
    val conflictResolutionState: StateFlow<Result<Unit>?> = _conflictResolutionState.asStateFlow()

    init {
        loadSyncStatus()
    }

    /**
     * Load the current sync status from the server.
     */
    fun loadSyncStatus() {
        viewModelScope.launch {
            val result = syncRepository.getSyncStatus()
            if (result.isSuccess) {
                _syncStatus.value = result.getOrNull()!!
            }
        }
    }

    /**
     * Perform a full sync (push then pull).
     */
    fun performFullSync() {
        viewModelScope.launch {
            _syncUiState.value = SyncUiState.Syncing
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                lastError = null,
            )

            val result = syncRepository.fullSync()
            if (result.isSuccess) {
                _syncStatus.value = result.getOrNull()!!.copy(isSyncing = false)
                _syncUiState.value = SyncUiState.Success
            } else {
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    lastError = result.exceptionOrNull()?.message,
                )
                _syncUiState.value = SyncUiState.Error(
                    result.exceptionOrNull()?.message ?: "Sync failed"
                )
            }
        }
    }

    /**
     * Push local changes to the server.
     */
    fun pushSync() {
        viewModelScope.launch {
            _syncUiState.value = SyncUiState.Syncing
            _syncStatus.value = _syncStatus.value.copy(isSyncing = true)

            val result = syncRepository.pushSync()
            if (result.isSuccess) {
                _syncStatus.value = result.getOrNull()!!.copy(isSyncing = false)
                _syncUiState.value = SyncUiState.Success
            } else {
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    lastError = result.exceptionOrNull()?.message,
                )
                _syncUiState.value = SyncUiState.Error(
                    result.exceptionOrNull()?.message ?: "Push sync failed"
                )
            }
        }
    }

    /**
     * Pull server changes to local.
     */
    fun pullSync() {
        viewModelScope.launch {
            _syncUiState.value = SyncUiState.Syncing
            _syncStatus.value = _syncStatus.value.copy(isSyncing = true)

            val result = syncRepository.pullSync()
            if (result.isSuccess) {
                _syncStatus.value = result.getOrNull()!!.copy(isSyncing = false)
                _syncUiState.value = SyncUiState.Success
            } else {
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    lastError = result.exceptionOrNull()?.message,
                )
                _syncUiState.value = SyncUiState.Error(
                    result.exceptionOrNull()?.message ?: "Pull sync failed"
                )
            }
        }
    }

    /**
     * Load conflict logs from the server.
     */
    fun loadConflicts() {
        viewModelScope.launch {
            val result = syncRepository.getConflicts()
            if (result.isSuccess) {
                _conflicts.value = result.getOrNull()!!
            }
        }
    }

    /**
     * Resolve a specific conflict.
     */
    fun resolveConflict(conflictId: Long, resolution: ConflictResolution) {
        viewModelScope.launch {
            val result = syncRepository.resolveConflict(conflictId, resolution)
            _conflictResolutionState.value = result
            if (result.isSuccess) {
                // Reload conflicts after resolution
                loadConflicts()
                loadSyncStatus()
            }
        }
    }

    /**
     * Clear the conflict resolution state after the UI has consumed it.
     */
    fun clearConflictResolutionState() {
        _conflictResolutionState.value = null
    }

    /**
     * Reset sync UI state to idle.
     */
    fun resetSyncUiState() {
        _syncUiState.value = SyncUiState.Idle
    }
}
