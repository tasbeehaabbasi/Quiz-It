package com.example.data.repository

import android.util.Log
import com.example.data.model.QuizAttempt
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class QuizAttemptRepository {
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        Log.w("QuizAttemptRepository", "Firebase not initialized. Make sure google-services.json is present.")
        null
    }
    private val attemptsCollection = firestore?.collection("quiz_attempts")

    suspend fun saveAttempt(attempt: QuizAttempt): Result<Unit> {
        if (attemptsCollection == null) return Result.failure(Exception("Firebase is not initialized"))
        return try {
            val id = if (attempt.id.isEmpty()) UUID.randomUUID().toString() else attempt.id
            val finalAttempt = attempt.copy(id = id)
            attemptsCollection.document(id).set(finalAttempt).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserAttempts(userId: String): Result<List<QuizAttempt>> {
        if (attemptsCollection == null) return Result.success(emptyList())
        return try {
            val snapshot = attemptsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val attempts = snapshot.toObjects(QuizAttempt::class.java).sortedByDescending { it.timestamp }
            Result.success(attempts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
