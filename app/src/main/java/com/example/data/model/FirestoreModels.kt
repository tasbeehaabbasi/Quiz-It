package com.example.data.model

data class User(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null
)

data class QuizAttempt(
    val id: String = "",
    val userId: String = "",
    val topic: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
