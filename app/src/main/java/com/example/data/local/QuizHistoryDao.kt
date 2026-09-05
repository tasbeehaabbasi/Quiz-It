package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizHistoryDao {
    @Query("SELECT * FROM quiz_history ORDER BY dateTaken DESC")
    fun getAllHistory(): Flow<List<QuizHistoryEntity>>

    @Query("SELECT * FROM quiz_history WHERE id = :id")
    suspend fun getQuizHistoryById(id: Int): QuizHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizHistory(quizHistory: QuizHistoryEntity)
}
