package com.smartnotes.ui.screens.notes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartnotes.ui.components.ConfirmDialog
import com.smartnotes.ui.components.EmptyState
import com.smartnotes.ui.components.ErrorMessage
import com.smartnotes.ui.components.LoadingIndicator
import com.smartnotes.ui.components.NavigationBackButton
import com.smartnotes.ui.components.NoteType
import com.smartnotes.ui.components.NoteTypeBadge
import com.smartnotes.ui.components.SmartNotesTopAppBar
import com.smartnotes.ui.viewmodel.NotesUiState
import com.smartnotes.ui.viewmodel.NoteFilter
import com.smartnotes.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNavigateToNoteEdit: (noteId: Long?) -> Unit,
    viewModel: NoteViewModel = hiltViewModel(),
) {
    val notesState by viewModel.notesState
    val isRefreshing by viewModel.isRefreshing
    val searchQuery by viewModel.searchQuery
    val activeFilter by viewModel.activeFilter
    val deleteConfirmNoteId by viewModel.deleteConfirmNoteId
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Load more on scroll to bottom
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            SmartNotesTopAppBar(
                title = "My Notes",
                actions = {
                    IconButton(onClick = { viewModel.syncNotes() }) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Sync notes",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToNoteEdit(null) },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new note",
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search notes...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                ),
                shape = MaterialTheme.shapes.large,
            )

            // Filter tabs
            val filters = NoteFilter.entries
            ScrollableFilterTabs(
                filters = filters,
                activeFilter = activeFilter,
                onFilterSelected = { viewModel.setFilter(it) },
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Content
            when (notesState) {
                is NotesUiState.Loading -> {
                    LoadingIndicator(message = "Loading notes...")
                }

                is NotesUiState.Error -> {
                    ErrorMessage(
                        message = (notesState as NotesUiState.Error).message,
                        onRetry = { viewModel.refreshNotes() },
                    )
                }

                is NotesUiState.Success -> {
                    val notes = (notesState as NotesUiState.Success).notes
                    if (notes.isEmpty()) {
                        EmptyState(
                            message = if (searchQuery.isBlank()) {
                                "No notes yet. Tap + to create one!"
                            } else {
                                "No notes match your search"
                            },
                            icon = Icons.Default.Create,
                            actionLabel = if (searchQuery.isBlank()) "Create Note" else null,
                            onAction = if (searchQuery.isBlank()) {
                                { onNavigateToNoteEdit(null) }
                            } else {
                                null
                            },
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 80.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = notes,
                                key = { note -> note.clientId },
                            ) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { onNavigateToNoteEdit(note.id) },
                                    onPinToggle = { viewModel.togglePin(note) },
                                    onDelete = { viewModel.showDeleteConfirm(note.id) },
                                )
                            }

                            // Loading more indicator
                            if ((notesState as NotesUiState.Success).hasMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pull-to-refresh indicator
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Delete confirmation dialog
        if (deleteConfirmNoteId != null) {
            ConfirmDialog(
                title = "Delete Note",
                message = "Are you sure you want to delete this note? This action cannot be undone.",
                onConfirm = { viewModel.deleteNote(deleteConfirmNoteId!!) },
                onDismiss = { viewModel.dismissDeleteConfirm() },
                confirmButtonText = "Delete",
            )
        }
    }
}

@Composable
private fun ScrollableFilterTabs(
    filters: List<NoteFilter>,
    activeFilter: NoteFilter,
    onFilterSelected: (NoteFilter) -> Unit,
) {
    androidx.compose.foundation.horizontalScroll
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            val isSelected = filter == activeFilter
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                label = "filterBg",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "filterContent",
            )

            Surface(
                modifier = Modifier.clip(MaterialTheme.shapes.small),
                color = backgroundColor,
            ) {
                Text(
                    text = filter.label,
                    modifier = Modifier
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: com.smartnotes.ui.viewmodel.Note,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (note.isPinned) 4.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NoteTypeBadge(type = note.type)
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            modifier = Modifier.size(18.dp),
                            tint = if (note.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatNoteDate(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

private fun formatNoteDate(dateTime: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    return if (dateTime.toLocalDate() == now.toLocalDate()) {
        "Today, ${dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
    } else if (dateTime.toLocalDate() == now.minusDays(1).toLocalDate()) {
        "Yesterday, ${dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
    } else {
        dateTime.format(formatter)
    }
}

@Composable
private fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progress: () -> Float = { 1f },
) {
    androidx.compose.material3.LinearProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = trackColor,
    )
}
