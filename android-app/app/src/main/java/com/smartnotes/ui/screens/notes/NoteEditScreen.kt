package com.smartnotes.ui.screens.notes

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
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    val noteEditState by viewModel.noteEditState
    val isEditMode = noteId != null && noteId > 0

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
        null to "No Repeat",
        "DAILY" to "Daily",
        "WEEKLY" to "Weekly",
        "MONTHLY" to "Monthly",
        "YEARLY" to "Yearly",
    )

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Note" else "New Note",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    NavigationBackButton(onBackClick = onNavigateBack)
                },
                actions = {
                    IconButton(
                        onClick = {
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
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save note",
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
                LoadingIndicator(message = if (isEditMode) "Loading note..." else null)
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
                        label = { Text("Title") },
                        placeholder = { Text("Enter note title") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Content (hidden for checklist type)
                    if (selectedType != NoteType.CHECKLIST) {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("Content") },
                            placeholder = {
                                Text(
                                    when (selectedType) {
                                        NoteType.NORMAL -> "Write your note here..."
                                        NoteType.REMINDER -> "Add reminder details..."
                                        NoteType.SECRET -> "Write your secret note..."
                                        else -> "Write content here..."
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
                            label = { Text("Note Type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
                            text = "Checklist Items",
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
                                            contentDescription = "Remove item",
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
                                placeholder = { Text("New item...") },
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
                                    contentDescription = "Add item",
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
                            text = "Reminder Settings",
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
                                } ?: "Select date and time",
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Repeat rule selector
                        ExposedDropdownMenuBox(
                            expanded = repeatDropdownExpanded,
                            onExpandedChange = { repeatDropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = reminderRepeatRule ?: "No Repeat",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Repeat") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
                            text = "Encryption",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Encryption Password") },
                            placeholder = { Text("Enter password to encrypt this note") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "This note will be encrypted with your password. You will need this password to view it later.",
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
                                    Text("Pin to Top", style = MaterialTheme.typography.bodyMedium)
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
        }
    }
}
