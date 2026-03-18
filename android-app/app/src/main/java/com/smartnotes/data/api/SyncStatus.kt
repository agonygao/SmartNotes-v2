package com.smartnotes.data.api

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class SyncStatus(val icon: ImageVector, val labelKey: String) {
    SYNCING(Icons.Default.CloudSync, "sync_status_syncing"),
    SYNCED(Icons.Default.CloudDone, "sync_status_synced"),
    OFFLINE(Icons.Default.CloudOff, "sync_status_offline"),
    CONFLICT(Icons.Default.Warning, "sync_status_conflict"),
    ERROR(Icons.Default.ErrorOutline, "sync_status_error"),
}
