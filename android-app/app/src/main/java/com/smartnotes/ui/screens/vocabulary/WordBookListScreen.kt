package com.smartnotes.ui.screens.vocabulary

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartnotes.ui.components.EmptyState
import com.smartnotes.ui.components.ErrorMessage
import com.smartnotes.ui.components.LoadingIndicator
import com.smartnotes.ui.components.SmartNotesTopAppBar
import com.smartnotes.ui.viewmodel.WordBook
import com.smartnotes.ui.viewmodel.WordBookType
import com.smartnotes.ui.viewmodel.WordBookViewModel
import com.smartnotes.ui.viewmodel.WordBooksUiState

@Composable
fun WordBookListScreen(
    onNavigateToWordBookDetail: (bookId: Long) -> Unit,
    viewModel: WordBookViewModel = hiltViewModel(),
) {
    val wordBooksState by viewModel.wordBooksState
    val isOperationLoading by viewModel.isOperationLoading
    val operationError by viewModel.operationError
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Show operation errors
    LaunchedEffect(operationError) {
        if (operationError != null) {
            snackbarHostState.showSnackbar(operationError!!)
            viewModel.clearOperationError()
        }
    }

    Scaffold(
        topBar = {
            SmartNotesTopAppBar(title = "Word Books")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new word book",
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when (wordBooksState) {
            is WordBooksUiState.Loading -> {
                LoadingIndicator(message = "Loading word books...")
            }

            is WordBooksUiState.Error -> {
                ErrorMessage(
                    message = (wordBooksState as WordBooksUiState.Error).message,
                    onRetry = { viewModel.loadWordBooks() },
                )
            }

            is WordBooksUiState.Success -> {
                val wordBooks = (wordBooksState as WordBooksUiState.Success).wordBooks
                if (wordBooks.isEmpty()) {
                    EmptyState(
                        message = "No word books yet. Tap + to create one!",
                        icon = Icons.Default.Book,
                        actionLabel = "Create Word Book",
                        onAction = { showCreateDialog = true },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 80.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Default books first (CET4, CET6)
                        val defaultBooks = wordBooks.filter { it.isDefault }
                        val customBooks = wordBooks.filter { !it.isDefault }

                        if (defaultBooks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Default Books",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(defaultBooks, key = { it.id }) { book ->
                                WordBookCard(
                                    wordBook = book,
                                    onClick = { onNavigateToWordBookDetail(book.id) },
                                )
                            }
                        }

                        if (customBooks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "My Books",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(customBooks, key = { it.id }) { book ->
                                WordBookCard(
                                    wordBook = book,
                                    onClick = { onNavigateToWordBookDetail(book.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Create word book dialog
        if (showCreateDialog) {
            CreateWordBookDialog(
                isLoading = isOperationLoading,
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, description ->
                    viewModel.createWordBook(name, description)
                    showCreateDialog = false
                },
            )
        }
    }
}

@Composable
private fun WordBookCard(
    wordBook: WordBook,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Book icon with color based on type
            val (icon, color) = when (wordBook.type) {
                WordBookType.CET4 -> Icons.Default.Book to Color(0xFF1976D2)
                WordBookType.CET6 -> Icons.Default.Book to Color(0xFF7B1FA2)
                WordBookType.CUSTOM -> Icons.Default.CreateNewFolder to Color(0xFF388E3C)
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = color.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = wordBook.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!wordBook.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = wordBook.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${wordBook.wordCount} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Type badge
            Surface(
                modifier = Modifier.clip(MaterialTheme.shapes.small),
                color = color.copy(alpha = 0.15f),
            ) {
                Text(
                    text = wordBook.type.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CreateWordBookDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Create Word Book",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Enter word book name") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Brief description") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), description.trim().ifBlank { null }) },
                enabled = !isLoading && name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        },
    )
}
