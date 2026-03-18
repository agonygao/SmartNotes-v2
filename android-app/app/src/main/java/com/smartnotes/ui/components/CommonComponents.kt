package com.smartnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartnotes.R

// ---------------------------------------------------------------------------
// Data class for note type badge colors
// ---------------------------------------------------------------------------
data class NoteTypeColor(val label: String, val backgroundColor: Color, val textColor: Color)

// ---------------------------------------------------------------------------
// SmartNotesTopAppBar
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartNotesTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable (() -> Unit) = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = { navigationIcon?.invoke() },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
fun NavigationBackButton(onBackClick: () -> Unit) {
    IconButton(onClick = onBackClick) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Navigate back",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

// ---------------------------------------------------------------------------
// LoadingIndicator
// ---------------------------------------------------------------------------
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            if (message != null) {
                Spacer(modifier = Modifier.padding(top = 12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ErrorMessage
// ---------------------------------------------------------------------------
@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Text(text = "Retry")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// EmptyState
// ---------------------------------------------------------------------------
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.InsertDriveFile,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.padding(top = 4.dp))
                OutlinedButton(onClick = onAction) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ConfirmDialog
// ---------------------------------------------------------------------------
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Confirm",
    dismissButtonText: String = "Cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmButtonText,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissButtonText)
            }
        },
    )
}

// ---------------------------------------------------------------------------
// NoteTypeBadge
// ---------------------------------------------------------------------------
enum class NoteType(val label: String, val color: Color) {
    NORMAL("Normal", Color(0xFF4CAF50)),
    CHECKLIST("Checklist", Color(0xFF2196F3)),
    REMINDER("Reminder", Color(0xFFFF9800)),
    SECRET("Secret", Color(0xFF9C27B0)),
}

@Composable
fun NoteTypeBadge(
    type: NoteType,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        color = type.color.copy(alpha = 0.15f),
    ) {
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelSmall,
            color = type.color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// FileTypeIcon
// ---------------------------------------------------------------------------
@Composable
fun FileTypeIcon(
    fileType: String,
    modifier: Modifier = Modifier,
) {
    val (icon, color) = when (fileType.lowercase()) {
        "pdf" -> Icons.Default.PictureAsPdf to Color(0xFFE53935)
        "doc", "docx" -> Icons.Default.Description to Color(0xFF1976D2)
        "xls", "xlsx" -> Icons.Default.TableChart to Color(0xFF388E3C)
        "txt" -> Icons.Default.InsertDriveFile to Color(0xFF757575)
        "md" -> Icons.Default.Description to Color(0xFF546E7A)
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> Icons.Default.Image to Color(0xFFFB8C00)
        else -> Icons.Default.InsertDriveFile to Color(0xFF9E9E9E)
    }

    Surface(
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = fileType,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SyncStatusChip
// ---------------------------------------------------------------------------
@Composable
fun SyncStatusChip(
    status: com.smartnotes.data.api.SyncStatus,
    modifier: Modifier = Modifier,
    conflictCount: Int = 0,
) {
    val label = when (status) {
        com.smartnotes.data.api.SyncStatus.SYNCING -> stringResource(R.string.sync_status_syncing)
        com.smartnotes.data.api.SyncStatus.SYNCED -> stringResource(R.string.sync_status_synced)
        com.smartnotes.data.api.SyncStatus.OFFLINE -> stringResource(R.string.sync_status_offline)
        com.smartnotes.data.api.SyncStatus.CONFLICT -> stringResource(R.string.sync_status_conflict, conflictCount)
        com.smartnotes.data.api.SyncStatus.ERROR -> stringResource(R.string.sync_status_error)
    }
    val containerColor = when (status) {
        com.smartnotes.data.api.SyncStatus.SYNCING -> MaterialTheme.colorScheme.primaryContainer
        com.smartnotes.data.api.SyncStatus.SYNCED -> MaterialTheme.colorScheme.secondaryContainer
        com.smartnotes.data.api.SyncStatus.OFFLINE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        com.smartnotes.data.api.SyncStatus.CONFLICT -> MaterialTheme.colorScheme.tertiaryContainer
        com.smartnotes.data.api.SyncStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (status) {
        com.smartnotes.data.api.SyncStatus.SYNCING -> MaterialTheme.colorScheme.onPrimaryContainer
        com.smartnotes.data.api.SyncStatus.SYNCED -> MaterialTheme.colorScheme.onSecondaryContainer
        com.smartnotes.data.api.SyncStatus.OFFLINE -> MaterialTheme.colorScheme.onErrorContainer
        com.smartnotes.data.api.SyncStatus.CONFLICT -> MaterialTheme.colorScheme.onTertiaryContainer
        com.smartnotes.data.api.SyncStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = status.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor,
            )
        },
        modifier = modifier.height(28.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            leadingIconContentColor = contentColor,
        ),
        border = null,
    )
}

// ---------------------------------------------------------------------------
// NetworkError
// ---------------------------------------------------------------------------
@Composable
fun NetworkError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = stringResource(R.string.error_network_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}
