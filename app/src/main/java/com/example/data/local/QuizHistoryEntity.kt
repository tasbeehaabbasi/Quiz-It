package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "quiz_history")
data class QuizHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val format: String,
    val questionCount: Int,
    val dateTaken: Long = System.currentTimeMillis(),
    val score: Int,
    val accuracy: Int,
    val questionsJson: String 
)

@JsonClass(generateAdapter = true)
data class QuestionHistory(
    val questionText: String,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean
)
