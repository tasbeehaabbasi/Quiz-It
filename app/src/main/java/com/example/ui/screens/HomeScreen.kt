package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.QuizState
import com.example.ui.viewmodel.QuizViewModel
import com.example.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    quizViewModel: QuizViewModel,
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    onNavigateToQuiz: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    var sourceText by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("single_choice") }
    var selectedCount by remember { mutableStateOf(10) }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    
    var showSettings by remember { mutableStateOf(false) }
    val themeMode by themeViewModel.themeMode.collectAsState()
    
    val wordCount = sourceText.split(Regex("\\s+")).count { it.isNotEmpty() }
    val isGenerateEnabled = wordCount in 50..5000
    
    val quizState by quizViewModel.quizState.collectAsState()
    
    LaunchedEffect(quizState) {
        if (quizState == QuizState.READY) {
            quizViewModel.startQuiz()
            onNavigateToQuiz()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(
                        text = "QuizIt",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showSettings,
                            onDismissRequest = { showSettings = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("System Theme") },
                                onClick = { themeViewModel.setThemeMode(0); showSettings = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Light Theme") },
                                onClick = { themeViewModel.setThemeMode(1); showSettings = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Dark Theme") },
                                onClick = { themeViewModel.setThemeMode(2); showSettings = false }
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToLibrary,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Library", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        },
        bottomBar = {
            if (quizState != QuizState.GENERATING) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                quizViewModel.generateQuiz(sourceText, selectedFormat, selectedCount, selectedDifficulty)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = CircleShape,
                            enabled = isGenerateEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Quiz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Powered by Gemini AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (quizState == QuizState.GENERATING) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                    Text("Creating your mastery quiz...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    
                    // Notes Input Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Study Material",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = sourceText,
                                onValueChange = { sourceText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                placeholder = { Text("Paste your notes, articles, or text here...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val counterColor = if (wordCount > 5000 || wordCount < 50) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                Text(
                                    text = "$wordCount / 5,000 words",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = counterColor
                                )
                            }
                        }
                    }

                    // Quiz Settings
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        ConfigSection(
                            title = "Format",
                            options = listOf("MCQ" to "single_choice", "True/False" to "true_false", "Short Answer" to "short_answer"),
                            selected = selectedFormat,
                            onSelect = { selectedFormat = it as String }
                        )
                        ConfigSection(
                            title = "Questions",
                            options = listOf("5" to 5, "10" to 10, "15" to 15),
                            selected = selectedCount,
                            onSelect = { selectedCount = it as Int }
                        )
                        ConfigSection(
                            title = "Difficulty",
                            options = listOf("Easy" to "Easy", "Medium" to "Medium", "Hard" to "Hard"),
                            selected = selectedDifficulty,
                            onSelect = { selectedDifficulty = it as String }
                        )
                    }
                    
                    // Example Topics
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Or try an example topic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val topics = listOf(
                            "Photosynthesis" to MaterialTheme.colorScheme.secondaryContainer,
                            "Roman Empire" to MaterialTheme.colorScheme.tertiaryContainer,
                            "Newton's Laws" to MaterialTheme.colorScheme.primaryContainer
                        )
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            topics.forEach { (topic, bgColor) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bgColor,
                                    modifier = Modifier.clickable { sourceText = getSampleText(topic) }
                                ) {
                                    Text(
                                        text = topic,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp)) // padding before bottom bar
                }
            }
        }
    }
}

@Composable
fun ConfigSection(
    title: String,
    options: List<Pair<String, Any>>,
    selected: Any,
    onSelect: (Any) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { (label, value) ->
                val isSelected = selected == value
                val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline
                
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor),
                    onClick = { onSelect(value) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

fun getSampleText(topic: String): String {
    return when (topic) {
        "Photosynthesis" -> "Photosynthesis is a process used by plants and other organisms to convert light energy into chemical energy that, through cellular respiration, can later be released to fuel the organism's activities. Some of this chemical energy is stored in carbohydrate molecules, such as sugars and starches, which are synthesized from carbon dioxide and water – hence the name photosynthesis, from the Greek phōs, light, and sunthesis, putting together."
        "Roman Empire" -> "The Roman Empire was the post-Republican period of ancient Rome. As a polity it included large territorial holdings around the Mediterranean Sea in Europe, Northern Africa, and Western Asia ruled by emperors. From the accession of Caesar Augustus to the military anarchy of the third century, it was a principate with Italy as metropole of the provinces and the city of Rome as sole capital."
        "Newton's Laws" -> "Newton's laws of motion are three basic laws of classical mechanics that describe the relationship between the motion of an object and the forces acting on it. These laws can be paraphrased as follows: A body remains at rest, or in motion at a constant speed in a straight line, unless acted upon by a force. When a body is acted upon by a force, the time rate of change of its momentum equals the force. If two bodies exert forces on each other, these forces have the same magnitude but opposite directions."
        else -> ""
    }
}
