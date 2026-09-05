package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QuizDatabase
import com.example.data.local.QuizHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val historyDao = QuizDatabase.getDatabase(application).quizHistoryDao()

    val attempts: StateFlow<List<QuizHistoryEntity>> = historyDao.getAllHistory()
        .onEach {
            android.util.Log.d("LibraryViewModel", "Fetched ${it.size} attempts from DB")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
