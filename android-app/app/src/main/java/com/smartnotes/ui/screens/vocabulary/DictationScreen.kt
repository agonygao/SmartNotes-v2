package com.smartnotes.ui.screens.vocabulary

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartnotes.ui.components.LoadingIndicator
import com.smartnotes.ui.components.NavigationBackButton
import com.smartnotes.ui.viewmodel.WordBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictationScreen(
    bookId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WordBookViewModel = hiltViewModel(),
) {
    val dictationSession by viewModel.dictationSession
    val isOperationLoading by viewModel.isOperationLoading

    // Start the dictation session
    LaunchedEffect(bookId) {
        viewModel.startDictationSession(bookId)
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = dictationSession?.bookName ?: "Dictation",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = { NavigationBackButton(onBackClick = onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        if (isOperationLoading && dictationSession == null) {
            LoadingIndicator(message = "Preparing dictation session...")
            return@Scaffold
        }

        if (dictationSession == null) {
            LoadingIndicator(message = "Loading...")
            return@Scaffold
        }

        val session = dictationSession!!

        if (session.isFinished) {
            // Results summary
            DictationResultsSummary(
                session = session,
                onDone = {
                    viewModel.submitDictationResults()
                    viewModel.resetDictationSession()
                    onNavigateBack()
                },
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            // Active dictation
            val currentWord = session.words.getOrNull(session.currentIndex)
            if (currentWord != null) {
                DictationInput(
                    session = session,
                    currentWord = currentWord,
                    onAnswerChange = { viewModel.updateDictationAnswer(it) },
                    onSubmit = { viewModel.submitDictationAnswer() },
                    onNext = { viewModel.nextDictationWord() },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun DictationInput(
    session: com.smartnotes.ui.viewmodel.DictationSession,
    currentWord: com.smartnotes.ui.viewmodel.Word,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = (session.currentIndex.toFloat()) / session.words.size,
        label = "progress",
    )

    val feedbackColor by animateColorAsState(
        targetValue = when {
            !session.showResult -> MaterialTheme.colorScheme.surface
            session.isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        },
        label = "feedbackColor",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Progress indicator
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${session.currentIndex + 1} of ${session.words.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Type the word you hear",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Score tracking
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Correct: ${session.correctCount}",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF4CAF50),
            )
            Text(
                text = "Wrong: ${session.wrongCount}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Phonetic display (the "prompt")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Listen and type:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentWord.phonetic ?: currentWord.word,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (!currentWord.meaning.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentWord.meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Answer input area with feedback
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = feedbackColor,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = session.currentAnswer,
                    onValueChange = {
                        if (!session.showResult) {
                            onAnswerChange(it)
                        }
                    },
                    placeholder = { Text("Type the word here...") },
                    singleLine = true,
                    enabled = !session.showResult,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    isError = session.showResult && !session.isCorrect,
                    supportingText = if (session.showResult && !session.isCorrect) {
                        { Text("Correct answer: ${currentWord.word}") }
                    } else {
                        null
                    },
                )

                // Submit or Next button
                if (!session.showResult) {
                    Button(
                        onClick = onSubmit,
                        enabled = session.currentAnswer.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(text = "Submit")
                    }
                } else {
                    // Feedback display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (session.isCorrect) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Close
                            },
                            contentDescription = null,
                            tint = if (session.isCorrect) {
                                Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (session.isCorrect) "Correct!" else "Incorrect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (session.isCorrect) {
                                Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(text = "Next Word")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DictationResultsSummary(
    session: com.smartnotes.ui.viewmodel.DictationSession,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val percentage = if (session.words.isNotEmpty()) {
        (session.correctCount.toFloat() / session.words.size * 100).toInt()
    } else {
        0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Dictation Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Score circle
        Card(
            modifier = Modifier.size(160.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Accuracy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${session.words.size}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${session.correctCount}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                )
                Text(
                    text = "Correct",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${session.wrongCount}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Wrong",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Wrong words list (if any)
        if (session.wrongCount > 0) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Words to review:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))

            val wrongResults = session.results.filter { !it.isCorrect }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                wrongResults.forEach { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = result.word.word,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "Your answer: ${result.userAnswer}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(text = "Done")
        }
    }
}
