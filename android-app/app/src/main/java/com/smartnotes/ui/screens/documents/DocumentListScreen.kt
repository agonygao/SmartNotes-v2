package com.smartnotes.ui.screens.documents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartnotes.ui.components.ConfirmDialog
import com.smartnotes.ui.components.EmptyState
import com.smartnotes.ui.components.ErrorMessage
import com.smartnotes.ui.components.FileTypeIcon
import com.smartnotes.ui.components.LoadingIndicator
import com.smartnotes.ui.components.SmartNotesTopAppBar
import com.smartnotes.ui.viewmodel.Document
import com.smartnotes.ui.viewmodel.DocumentViewModel
import com.smartnotes.ui.viewmodel.DocumentsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListScreen(
    onNavigateToUpload: () -> Unit,
    onNavigateToPreview: (docId: Long) -> Unit,
    viewModel: DocumentViewModel = hiltViewModel(),
) {
    val documentsState by viewModel.documentsState
    val deleteConfirmDocId by viewModel.deleteConfirmDocId
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }

    Scaffold(
        topBar = {
            SmartNotesTopAppBar(
                title = "Documents",
                actions = {
                    IconButton(onClick = {
                        viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                    }) {
                        Icon(
                            imageVector = if (viewMode == ViewMode.LIST) {
                                Icons.Default.GridView
                            } else {
                                Icons.Default.List
                            },
                            contentDescription = if (viewMode == ViewMode.LIST) {
                                "Switch to grid view"
                            } else {
                                "Switch to list view"
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToUpload,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload document",
                )
            }
        },
    ) { paddingValues ->
        when (documentsState) {
            is DocumentsUiState.Loading -> {
                LoadingIndicator(message = "Loading documents...")
            }

            is DocumentsUiState.Error -> {
                ErrorMessage(
                    message = (documentsState as DocumentsUiState.Error).message,
                    onRetry = { viewModel.loadDocuments() },
                )
            }

            is DocumentsUiState.Success -> {
                val documents = (documentsState as DocumentsUiState.Success).documents
                if (documents.isEmpty()) {
                    EmptyState(
                        message = "No documents yet. Tap + to upload one!",
                        icon = Icons.Default.InsertDriveFile,
                        actionLabel = "Upload Document",
                        onAction = onNavigateToUpload,
                    )
                } else {
                    if (viewMode == ViewMode.LIST) {
                        DocumentListView(
                            documents = documents,
                            paddingValues = paddingValues,
                            onDocumentClick = { onNavigateToPreview(it.id) },
                            onDocumentLongClick = { viewModel.showDeleteConfirm(it.id) },
                        )
                    } else {
                        DocumentGridView(
                            documents = documents,
                            paddingValues = paddingValues,
                            onDocumentClick = { onNavigateToPreview(it.id) },
                            onDocumentLongClick = { viewModel.showDeleteConfirm(it.id) },
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (deleteConfirmDocId != null) {
            ConfirmDialog(
                title = "Delete Document",
                message = "Are you sure you want to delete this document? This action cannot be undone.",
                onConfirm = { viewModel.deleteDocument(deleteConfirmDocId!!) },
                onDismiss = { viewModel.dismissDeleteConfirm() },
                confirmButtonText = "Delete",
            )
        }
    }
}

private enum class ViewMode {
    LIST, GRID
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentListView(
    documents: List<Document>,
    paddingValues: PaddingValues,
    onDocumentClick: (Document) -> Unit,
    onDocumentLongClick: (Document) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(documents, key = { it.id }) { document ->
            DocumentListItem(
                document = document,
                onClick = { onDocumentClick(document) },
                onLongClick = { onDocumentLongClick(document) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentListItem(
    document: Document,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileTypeIcon(
                fileType = document.fileType,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.originalFilename,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = document.fileType.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = formatFileSize(document.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = document.createdAt.format(
                        java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentGridView(
    documents: List<Document>,
    paddingValues: PaddingValues,
    onDocumentClick: (Document) -> Unit,
    onDocumentLongClick: (Document) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 80.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(documents, key = { it.id }) { document ->
            DocumentGridItem(
                document = document,
                onClick = { onDocumentClick(document) },
                onLongClick = { onDocumentLongClick(document) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentGridItem(
    document: Document,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FileTypeIcon(
                fileType = document.fileType,
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = document.originalFilename,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFileSize(document.fileSize),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }
}
