package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.QuizState
import com.example.ui.viewmodel.QuizViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizViewModel: QuizViewModel,
    onNavigateToResults: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val quizData by quizViewModel.quizData.collectAsState()
    val currentIndex by quizViewModel.currentQuestionIndex.collectAsState()
    val userAnswers by quizViewModel.userAnswers.collectAsState()
    val markedForReview by quizViewModel.markedForReview.collectAsState()
    val flaggedQuestions by quizViewModel.flaggedQuestions.collectAsState()
    val quizState by quizViewModel.quizState.collectAsState()
    
    var showExitDialog by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (quizState == QuizState.IN_PROGRESS) {
            delay(1000)
            timerSeconds++
        }
    }

    LaunchedEffect(quizState) {
        if (quizState == QuizState.SCORING || quizState == QuizState.RESULTS) {
            onNavigateToResults()
        }
    }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave quiz?") },
            text = { Text("Your progress will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    quizViewModel.reset()
                    onNavigateBack()
                }) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (quizState == QuizState.REVIEW) {
        ReviewScreen(quizViewModel = quizViewModel)
        return
    }

    if (quizData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val totalQuestions = quizData!!.questions.size
    val currentQuestion = quizData!!.questions[currentIndex]
    val currentAnswer = userAnswers[currentIndex] ?: ""
    val isMarked = markedForReview.contains(currentIndex)
    val isFlagged = flaggedQuestions.contains(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quizData!!.topic) },
                actions = {
                    Text(
                        text = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / totalQuestions },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Question ${currentIndex + 1} of $totalQuestions", style = MaterialTheme.typography.labelLarge)
                Text("${((currentIndex + 1) * 100) / totalQuestions}%", style = MaterialTheme.typography.labelLarge)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(currentQuestion.question, style = MaterialTheme.typography.headlineSmall)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (currentQuestion.type == "short_answer") {
                OutlinedTextField(
                    value = currentAnswer,
                    onValueChange = { quizViewModel.submitAnswer(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type your answer...") }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentQuestion.options ?: emptyList()) { option ->
                        val isSelected = option == currentAnswer
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { quizViewModel.submitAnswer(option) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = null)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(option)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = { quizViewModel.toggleReviewMark() }) {
                        Icon(
                            if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Mark for review",
                            tint = if (isMarked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { quizViewModel.toggleFlagQuestion() }) {
                        Icon(
                            if (isFlagged) Icons.Default.Flag else Icons.Outlined.Flag,
                            contentDescription = "Report issue",
                            tint = if (isFlagged) MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    }
                }
                
                Button(
                    onClick = { quizViewModel.nextQuestion() },
                    enabled = currentAnswer.isNotBlank()
                ) {
                    Text(if (currentIndex == totalQuestions - 1) "Review" else "Next Question")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(quizViewModel: QuizViewModel) {
    val quizData by quizViewModel.quizData.collectAsState()
    val userAnswers by quizViewModel.userAnswers.collectAsState()
    val markedForReview by quizViewModel.markedForReview.collectAsState()
    val flaggedQuestions by quizViewModel.flaggedQuestions.collectAsState()

    if (quizData == null) return

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Review Answers") })
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = { quizViewModel.submitQuiz() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)
                ) {
                    Text("Submit Quiz", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quizData!!.questions.size) { index ->
                val question = quizData!!.questions[index]
                val answer = userAnswers[index] ?: ""
                val isMarked = markedForReview.contains(index)
                val isFlagged = flaggedQuestions.contains(index)
                
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { quizViewModel.jumpToQuestion(index) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Question ${index + 1}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isMarked) Icon(Icons.Default.Bookmark, contentDescription = "Marked", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                if (isFlagged) Icon(Icons.Default.Flag, contentDescription = "Flagged", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(question.question, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (answer.isBlank()) "Not answered" else "Selected: $answer", 
                            color = if (answer.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}
