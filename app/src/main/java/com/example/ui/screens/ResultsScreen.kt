package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.QuizState
import com.example.ui.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    quizViewModel: QuizViewModel,
    userId: String,
    onNavigateHome: () -> Unit,
    onRetakeQuiz: () -> Unit
) {
    val quizState by quizViewModel.quizState.collectAsState()
    val quizData by quizViewModel.quizData.collectAsState()
    val feedbackData by quizViewModel.feedbackData.collectAsState()
    val errorMessage by quizViewModel.errorMessage.collectAsState()
    val userAnswers by quizViewModel.userAnswers.collectAsState()

    LaunchedEffect(quizState) {
        if (quizState == QuizState.SCORING) {
            quizViewModel.saveAttempt(userId)
        }
    }

    if (quizState == QuizState.SCORING) {
        Scaffold { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scoring and generating feedback...")
                }
            }
        }
        return
    }
    
    if (quizState == QuizState.ERROR) {
        Scaffold { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateHome) {
                        Text("Return Home")
                    }
                }
            }
        }
        return
    }

    val currentQuizData = quizData
    val currentFeedback = feedbackData
    if (currentQuizData == null || currentFeedback == null) return

    val score = quizViewModel.getScore()
    val total = currentQuizData.questions.size
    val scorePercent = if (total > 0) (score * 100) / total else 0
    val timeTaken = quizViewModel.getTimeTakenSeconds()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Quiz Results") }) },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            quizViewModel.startQuiz()
                            onRetakeQuiz()
                        },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Retake Quiz")
                    }
                    Button(
                        onClick = onNavigateHome,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("New Quiz")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                        CircularProgressIndicator(
                            progress = { scorePercent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 12.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text("$scorePercent%", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val shortMessage = currentFeedback.mastery_message.split(".").firstOrNull() ?: "$scorePercent% Mastery"
                    Text("$scorePercent% Mastery — $shortMessage", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatChip("Correct", "$score / $total")
                        StatChip("Time", String.format("%02d:%02d", timeTaken / 60, timeTaken % 60))
                    }
                }
            }

            item {
                ExpandableFeedbackCard(
                    title = "Concept to Review",
                    subtitle = currentFeedback.weak_concept_title,
                    content = currentFeedback.weak_concept_subtext,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }

            item {
                ExpandableFeedbackCard(
                    title = "Study Tip",
                    subtitle = "Tap to view a custom study tip",
                    content = currentFeedback.study_tip,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    icon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Question Breakdown", style = MaterialTheme.typography.titleMedium)
            }

            itemsIndexed(currentQuizData.questions) { index, question ->
                val userAnswer = userAnswers[index] ?: ""
                val isCorrect = quizViewModel.checkAnswer(question, userAnswer)
                val isMarked = quizViewModel.markedForReview.value.contains(index)
                val isFlagged = quizViewModel.flaggedQuestions.value.contains(index)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(question.question, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (isMarked || isFlagged) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 8.dp)) {
                                    if (isMarked) Icon(Icons.Default.Bookmark, contentDescription = "Marked", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    if (isFlagged) Icon(Icons.Default.Flag, contentDescription = "Flagged", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Your answer: ${if (userAnswer.isBlank()) "No answer" else userAnswer}", style = MaterialTheme.typography.bodySmall)
                        if (!isCorrect) {
                            Text("Correct answer: ${question.correct_answer}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableFeedbackCard(
    title: String,
    subtitle: String,
    content: String,
    containerColor: Color,
    contentColor: Color,
    icon: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = contentColor)
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(content, style = MaterialTheme.typography.bodySmall, color = contentColor)
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
