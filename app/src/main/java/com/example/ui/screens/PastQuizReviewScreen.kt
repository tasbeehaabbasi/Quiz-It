package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.QuestionHistory
import com.example.data.local.QuizDatabase
import com.example.data.local.QuizHistoryEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastQuizReviewScreen(
    quizId: Int,
    database: QuizDatabase,
    onNavigateBack: () -> Unit
) {
    var quizHistory by remember { mutableStateOf<QuizHistoryEntity?>(null) }
    var questions by remember { mutableStateOf<List<QuestionHistory>>(emptyList()) }

    LaunchedEffect(quizId) {
        withContext(Dispatchers.IO) {
            val history = database.quizHistoryDao().getQuizHistoryById(quizId)
            quizHistory = history
            
            if (history != null) {
                val moshi = Moshi.Builder().build()
                val type = Types.newParameterizedType(List::class.java, QuestionHistory::class.java)
                val adapter = moshi.adapter<List<QuestionHistory>>(type)
                questions = adapter.fromJson(history.questionsJson) ?: emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Review") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        val history = quizHistory
        if (history == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
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
                        Text(
                            text = "${history.topic} — ${history.format}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val dateStr = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(history.dateTaken))
                        Text(text = "Taken on $dateStr", style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatChip("Correct", "${history.score} / ${history.questionCount}")
                            StatChip("Accuracy", "${history.accuracy}%")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Question Breakdown", style = MaterialTheme.typography.titleMedium)
                }

                itemsIndexed(questions) { index, question ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (question.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (question.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(question.questionText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your answer: ${if (question.userAnswer.isBlank()) "No answer" else question.userAnswer}", style = MaterialTheme.typography.bodySmall)
                            if (!question.isCorrect) {
                                Text("Correct answer: ${question.correctAnswer}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
