package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.DifficultyEasy
import com.example.ui.theme.DifficultyHard
import com.example.ui.theme.DifficultyMedium
import com.example.ui.viewmodel.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: (Int) -> Unit
) {
    val attempts by libraryViewModel.attempts.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Past Quizzes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (attempts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Empty state illustration
                // Note: The drawable name is dynamic, I will replace it with the actual one in the next edit
                Image(
                    painter = painterResource(id = R.drawable.empty_state_illustration_1788594742779),
                    contentDescription = "No past quizzes",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No past quizzes yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Generate your first quiz from your notes and track your mastery here!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.height(56.dp).fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Text("Create a Quiz", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(attempts) { attempt ->
                    val scoreColor = when {
                        attempt.accuracy >= 80 -> DifficultyEasy
                        attempt.accuracy >= 50 -> DifficultyMedium
                        else -> DifficultyHard
                    }
                    val isDark = isSystemInDarkTheme()
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToReview(attempt.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp),
                        border = if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (attempt.format) {
                                        "single_choice" -> Icons.Default.FormatListBulleted
                                        "true_false" -> Icons.Default.CheckCircle
                                        else -> Icons.Default.Assignment
                                    }
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Center Text
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = attempt.topic.ifEmpty { "General Quiz" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val formatLabel = when (attempt.format) {
                                        "single_choice" -> "MCQ"
                                        "true_false" -> "True/False"
                                        else -> "Short Answer"
                                    }
                                    Text(
                                        text = "$formatLabel • ${attempt.questionCount} Questions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Right Score Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = scoreColor.copy(alpha = 0.2f),
                                ) {
                                    Text(
                                        text = "${attempt.accuracy}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(attempt.dateTaken))
                            Text(
                                text = date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
