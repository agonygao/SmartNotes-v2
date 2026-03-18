package com.smartnotes.ui.screens.notes

import com.smartnotes.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartnotes.ui.components.ErrorMessage
import com.smartnotes.ui.components.LoadingIndicator
import com.smartnotes.ui.components.NavigationBackButton
import com.smartnotes.ui.components.NoteType
import com.smartnotes.ui.viewmodel.ChecklistItem
import com.smartnotes.ui.viewmodel.NoteEditUiState
import com.smartnotes.ui.viewmodel.NoteViewModel
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel(),
) {
    val noteEditState = viewModel.noteEditState.collectAsState().value
    val isEditMode = noteId != null && noteId > 0
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Form fields
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NoteType.NORMAL) }
    var checklistItems by remember { mutableStateOf(listOf<ChecklistItem>()) }
    var reminderDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var reminderRepeatRule by remember { mutableStateOf<String?>(null) }
    var isPinned by remember { mutableStateOf(false) }
    var isEncrypted by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var showEncryptConfirmDialog by remember { mutableStateOf(false) }

    // Show save errors via snackbar
    LaunchedEffect(noteEditState) {
        if (noteEditState is NoteEditUiState.Error) {
            val rawMessage = (noteEditState as NoteEditUiState.Error).message
            val message = when {
                rawMessage.contains("decrypt", ignoreCase = true) ||
                    rawMessage.contains("cipher", ignoreCase = true) ||
                    rawMessage.contains("password", ignoreCase = true) ->
                    stringResource(R.string.encryption_decrypt_failed)
                rawMessage.contains("encrypt", ignoreCase = true) ->
                    stringResource(R.string.encryption_save_failed)
                else -> rawMessage
            }
            snackbarHostState.showSnackbar(message = message)
        }
    }

    // Checklist item being edited
    var newChecklistText by remember { mutableStateOf("") }

    // Load note data if editing
    LaunchedEffect(noteId) {
        if (isEditMode) {
            viewModel.loadNoteForEdit(noteId!!)
        }
    }

    // Populate fields when note is loaded
    LaunchedEffect(noteEditState) {
        if (noteEditState is NoteEditUiState.Loaded) {
            val note = (noteEditState as NoteEditUiState.Loaded).note
            title = note.title
            content = note.content
            selectedType = note.type
            checklistItems = note.checklistItems.toMutableList()
            reminderDate = note.reminderTime
            reminderRepeatRule = note.reminderRepeatRule
            isPinned = note.isPinned
            isEncrypted = note.isEncrypted
        }
    }

    // Handle save success
    LaunchedEffect(noteEditState) {
        if (noteEditState is NoteEditUiState.Saved) {
            onNavigateBack()
        }
    }

    // Type dropdown state
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    // Repeat rule dropdown state
    var repeatDropdownExpanded by remember { mutableStateOf(false) }
    val repeatOptions = listOf(
        null to stringResource(R.string.no_repeat),
        "DAILY" to stringResource(R.string.daily),
        "WEEKLY" to stringResource(R.string.weekly),
        "MONTHLY" to stringResource(R.string.monthly),
        "YEARLY" to stringResource(R.string.yearly),
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) stringResource(R.string.edit_note) else stringResource(R.string.new_note),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    NavigationBackButton(onBackClick = onNavigateBack)
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (selectedType == NoteType.SECRET) {
                                if (password.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.encryption_password_required)
                                        )
                                    }
                                } else if (password.length < 4) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.encryption_password_short)
                                        )
                                    }
                                } else {
                                    showEncryptConfirmDialog = true
                                }
                            } else {
                                viewModel.saveNote(
                                    noteId = noteId,
                                    title = title,
                                    content = content,
                                    type = selectedType,
                                    checklistItems = if (selectedType == NoteType.CHECKLIST) checklistItems else emptyList(),
                                    reminderTime = reminderDate,
                                    reminderRepeatRule = reminderRepeatRule,
                                    isPinned = isPinned,
                                    isEncrypted = isEncrypted,
                                    password = if (isEncrypted) password else null,
                                )
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.save),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        when (noteEditState) {
            is NoteEditUiState.Loading -> {
                LoadingIndicator(message = if (isEditMode) stringResource(R.string.loading_notes) else null)
            }
            is NoteEditUiState.Error -> {
                ErrorMessage(
                    message = (noteEditState as NoteEditUiState.Error).message,
                    onRetry = {
                        if (isEditMode) {
                            viewModel.loadNoteForEdit(noteId!!)
                        }
                    },
                )
            }
            else -> {
                // Edit / Create form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.title)) },
                        placeholder = { Text(stringResource(R.string.enter_title)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Content (hidden for checklist type)
                    if (selectedType != NoteType.CHECKLIST) {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text(stringResource(R.string.content)) },
                            placeholder = {
                                Text(
                                    when (selectedType) {
                                        NoteType.NORMAL -> stringResource(R.string.write_here)
                                        NoteType.REMINDER -> stringResource(R.string.write_here)
                                        NoteType.SECRET -> stringResource(R.string.write_here)
                                        else -> stringResource(R.string.write_here)
                                    }
                                )
                            },
                            minLines = 5,
                            maxLines = 20,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Note Type Selector
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedType.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.note_type)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false },
                        ) {
                            NoteType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        selectedType = type
                                        typeDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // ----------------------------------------------------------
                    // Checklist-specific UI
                    // ----------------------------------------------------------
                    if (selectedType == NoteType.CHECKLIST) {
                        Text(
                            text = stringResource(R.string.checklist_items),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )

                        // Existing checklist items
                        checklistItems.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = if (item.isChecked) 0.5f else 1f
                                    )
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Checkbox
                                    androidx.compose.material3.Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { checked ->
                                            val updatedItems = checklistItems.toMutableList()
                                            updatedItems[index] = updatedItems[index].copy(isChecked = checked)
                                            checklistItems = updatedItems
                                        },
                                    )
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = {
                                            checklistItems = checklistItems.filterIndexed { i, _ -> i != index }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.delete),
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }

                        // Add new checklist item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = newChecklistText,
                                onValueChange = { newChecklistText = it },
                                placeholder = { Text(stringResource(R.string.new_item)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    if (newChecklistText.isNotBlank()) {
                                        checklistItems = checklistItems + ChecklistItem(
                                            text = newChecklistText.trim(),
                                        )
                                        newChecklistText = ""
                                    }
                                },
                                enabled = newChecklistText.isNotBlank(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    // ----------------------------------------------------------
                    // Reminder-specific UI
                    // ----------------------------------------------------------
                    if (selectedType == NoteType.REMINDER) {
                        Text(
                            text = stringResource(R.string.reminder_settings),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )

                        // Date picker button
                        OutlinedButton(
                            onClick = {
                                // In a real implementation, this would open a DatePickerDialog
                                reminderDate = LocalDateTime.now().plusHours(1)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reminderDate?.let {
                                    it.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                } ?: stringResource(R.string.select_date_time),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Repeat rule selector
                        ExposedDropdownMenuBox(
                            expanded = repeatDropdownExpanded,
                            onExpandedChange = { repeatDropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = reminderRepeatRule ?: stringResource(R.string.no_repeat),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.repeat)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = repeatDropdownExpanded,
                                onDismissRequest = { repeatDropdownExpanded = false },
                            ) {
                                repeatOptions.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            reminderRepeatRule = value
                                            repeatDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // ----------------------------------------------------------
                    // Secret type - password for encryption
                    // ----------------------------------------------------------
                    if (selectedType == NoteType.SECRET) {
                        Text(
                            text = stringResource(R.string.encryption),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.encryption_password)) },
                            placeholder = { Text(stringResource(R.string.encryption_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.encryption_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // ----------------------------------------------------------
                    // Common toggles
                    // ----------------------------------------------------------
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Pin toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.pin_to_top), style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(
                                    checked = isPinned,
                                    onCheckedChange = { isPinned = it },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Encryption confirmation dialog
            if (showEncryptConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showEncryptConfirmDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.encryption_confirm_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.encryption_confirm_message),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showEncryptConfirmDialog = false
                                viewModel.saveNote(
                                    noteId = noteId,
                                    title = title,
                                    content = content,
                                    type = selectedType,
                                    checklistItems = if (selectedType == NoteType.CHECKLIST) checklistItems else emptyList(),
                                    reminderTime = reminderDate,
                                    reminderRepeatRule = reminderRepeatRule,
                                    isPinned = isPinned,
                                    isEncrypted = true,
                                    password = password.ifBlank { null },
                                )
                            },
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEncryptConfirmDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }
        }
    }
}
