package com.smartnotes.ui.screens.vocabulary

import com.smartnotes.R

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartnotes.ui.components.EmptyState
import com.smartnotes.ui.components.ErrorMessage
import com.smartnotes.ui.components.LoadingIndicator
import com.smartnotes.ui.components.NavigationBackButton
import com.smartnotes.ui.components.SmartNotesTopAppBar
import com.smartnotes.ui.viewmodel.Word
import com.smartnotes.ui.viewmodel.WordBookViewModel
import com.smartnotes.ui.viewmodel.WordsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordBookDetailScreen(
    bookId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToReview: (bookId: Long) -> Unit,
    onNavigateToDictation: (bookId: Long) -> Unit,
    viewModel: WordBookViewModel = hiltViewModel(),
) {
    val currentWordBook = viewModel.currentWordBook.collectAsState().value
    val wordsState = viewModel.wordsState.collectAsState().value
    val isOperationLoading = viewModel.isOperationLoading.collectAsState().value
    val operationError = viewModel.operationError.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddWordDialog by remember { mutableStateOf(false) }
    var expandedWordId by remember { mutableStateOf<Long?>(null) }

    // Load word book details
    LaunchedEffect(bookId) {
        viewModel.loadWordBookDetail(bookId)
    }

    // Clear current book on leave
    LaunchedEffect(Unit) {
        viewModel.clearCurrentBook()
    }

    // Show operation errors
    LaunchedEffect(operationError) {
        if (operationError != null) {
            snackbarHostState.showSnackbar(message = operationError!!)
            viewModel.clearOperationError()
        }
    }

    Scaffold(
        topBar = {
            SmartNotesTopAppBar(
                title = currentWordBook?.name ?: stringResource(R.string.word_book),
                navigationIcon = { NavigationBackButton(onBackClick = onNavigateBack) },
                actions = {
                    // Review button
                    IconButton(onClick = { onNavigateToReview(bookId) }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start review",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddWordDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add word",
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Word book header
            currentWordBook?.let { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = book.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!book.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "${book.wordCount} words",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = book.type.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Review and Dictation buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { onNavigateToReview(bookId) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.review))
                            }
                            OutlinedButton(
                                onClick = { onNavigateToDictation(bookId) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.dictation))
                            }
                        }
                    }
                }
            }

            // Words list
            when (wordsState) {
                is WordsUiState.Loading -> {
                    LoadingIndicator(message = stringResource(R.string.loading_words))
                }

                is WordsUiState.Error -> {
                    ErrorMessage(
                        message = (wordsState as WordsUiState.Error).message,
                        onRetry = { viewModel.loadWordBookDetail(bookId) },
                    )
                }

                is WordsUiState.Success -> {
                    val words = (wordsState as WordsUiState.Success).words
                    if (words.isEmpty()) {
                        EmptyState(
                            message = stringResource(R.string.no_words),
                            icon = Icons.Default.Book,
                            actionLabel = stringResource(R.string.add_word),
                            onAction = { showAddWordDialog = true },
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 80.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(words, key = { it.id }) { word ->
                                WordItemCard(
                                    word = word,
                                    isExpanded = expandedWordId == word.id,
                                    onClick = {
                                        expandedWordId = if (expandedWordId == word.id) null else word.id
                                    },
                                    onDelete = { viewModel.deleteWord(word.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add word dialog
        if (showAddWordDialog) {
            AddWordDialog(
                bookId = bookId,
                isLoading = isOperationLoading,
                onDismiss = { showAddWordDialog = false },
                onConfirm = { word ->
                    viewModel.addWord(word)
                    showAddWordDialog = false
                },
            )
        }
    }
}

@Composable
private fun WordItemCard(
    word: Word,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!word.phonetic.isNullOrBlank()) {
                        Text(
                            text = word.phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete word",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.ArrowDropUp
                        } else {
                            Icons.Default.ArrowDropDown
                        },
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                    )
                }
            }

            // Meaning (always visible as preview)
            if (!word.meaning.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Expanded: show example sentence
            if (isExpanded && !word.exampleSentence.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Example:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.exampleSentence,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
private fun AddWordDialog(
    bookId: Long,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (com.smartnotes.ui.viewmodel.Word) -> Unit,
) {
    var word by remember { mutableStateOf("") }
    var phonetic by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var exampleSentence by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.add_word),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("Word *") },
                    placeholder = { Text("e.g., abandon") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phonetic,
                    onValueChange = { phonetic = it },
                    label = { Text("Phonetic (optional)") },
                    placeholder = { Text("e.g., /əˈbændən/") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text("Meaning *") },
                    placeholder = { Text("e.g., v. to leave behind") },
                    minLines = 2,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = exampleSentence,
                    onValueChange = { exampleSentence = it },
                    label = { Text("Example Sentence (optional)") },
                    placeholder = { Text("e.g., They abandoned the sinking ship.") },
                    minLines = 2,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        com.smartnotes.ui.viewmodel.Word(
                            bookId = bookId,
                            word = word.trim(),
                            phonetic = phonetic.trim().ifBlank { null },
                            meaning = meaning.trim(),
                            exampleSentence = exampleSentence.trim().ifBlank { null },
                        )
                    )
                },
                enabled = !isLoading && word.isNotBlank() && meaning.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
