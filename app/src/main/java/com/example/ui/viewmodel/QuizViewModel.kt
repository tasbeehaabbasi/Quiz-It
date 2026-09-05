package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QuestionHistory
import com.example.data.local.QuizDatabase
import com.example.data.local.QuizHistoryEntity
import com.example.data.model.Question
import com.example.data.model.QuizFeedbackResponse
import com.example.data.model.QuizGenerationResponse
import com.example.data.repository.GeminiRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class QuizState {
    IDLE, GENERATING, READY, IN_PROGRESS, REVIEW, SCORING, RESULTS, ERROR
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val geminiRepository = GeminiRepository()
    private val historyDao = QuizDatabase.getDatabase(application).quizHistoryDao()
    
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, QuestionHistory::class.java)
    private val jsonAdapter = moshi.adapter<List<QuestionHistory>>(listType)

    private val _quizState = MutableStateFlow(QuizState.IDLE)
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val _quizData = MutableStateFlow<QuizGenerationResponse?>(null)
    val quizData: StateFlow<QuizGenerationResponse?> = _quizData.asStateFlow()

    private val _feedbackData = MutableStateFlow<QuizFeedbackResponse?>(null)
    val feedbackData: StateFlow<QuizFeedbackResponse?> = _feedbackData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Quiz taking state
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, String>> = _userAnswers.asStateFlow()
    
    private val _markedForReview = MutableStateFlow<Set<Int>>(emptySet())
    val markedForReview: StateFlow<Set<Int>> = _markedForReview.asStateFlow()

    private val _flaggedQuestions = MutableStateFlow<Set<Int>>(emptySet())
    val flaggedQuestions: StateFlow<Set<Int>> = _flaggedQuestions.asStateFlow()

    private var quizStartTime: Long = 0
    private var quizEndTime: Long = 0

    fun generateQuiz(sourceText: String, format: String, count: Int, difficulty: String) {
        viewModelScope.launch {
            _quizState.value = QuizState.GENERATING
            _errorMessage.value = null
            
            val result = geminiRepository.generateQuiz(sourceText, format, count, difficulty)
            result.onSuccess { data ->
                _quizData.value = data
                _quizState.value = QuizState.READY
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to generate quiz."
                _quizState.value = QuizState.ERROR
            }
        }
    }

    fun startQuiz() {
        _currentQuestionIndex.value = 0
        _userAnswers.value = emptyMap()
        _markedForReview.value = emptySet()
        _flaggedQuestions.value = emptySet()
        quizStartTime = System.currentTimeMillis()
        _quizState.value = QuizState.IN_PROGRESS
    }

    fun submitAnswer(answer: String) {
        val currentAnswers = _userAnswers.value.toMutableMap()
        currentAnswers[_currentQuestionIndex.value] = answer
        _userAnswers.value = currentAnswers
    }
    
    fun toggleReviewMark() {
        val currentMarks = _markedForReview.value.toMutableSet()
        val index = _currentQuestionIndex.value
        if (currentMarks.contains(index)) {
            currentMarks.remove(index)
        } else {
            currentMarks.add(index)
        }
        _markedForReview.value = currentMarks
    }

    fun toggleFlagQuestion() {
        val currentFlags = _flaggedQuestions.value.toMutableSet()
        val index = _currentQuestionIndex.value
        if (currentFlags.contains(index)) {
            currentFlags.remove(index)
        } else {
            currentFlags.add(index)
        }
        _flaggedQuestions.value = currentFlags
    }

    fun nextQuestion() {
        val total = _quizData.value?.questions?.size ?: 0
        if (_currentQuestionIndex.value < total - 1) {
            _currentQuestionIndex.value++
        } else {
            _quizState.value = QuizState.REVIEW
        }
    }

    fun jumpToQuestion(index: Int) {
        _currentQuestionIndex.value = index
        _quizState.value = QuizState.IN_PROGRESS
    }

    fun submitQuiz() {
        finishQuiz()
    }

    private fun finishQuiz() {
        quizEndTime = System.currentTimeMillis()
        _quizState.value = QuizState.SCORING
        
        viewModelScope.launch {
            val data = _quizData.value ?: return@launch
            val answers = _userAnswers.value
            
            var score = 0
            val incorrectConcepts = mutableListOf<String>()
            
            data.questions.forEachIndexed { index, question ->
                val userAnswer = answers[index] ?: ""
                val isCorrect = checkAnswer(question, userAnswer)
                if (isCorrect) {
                    score++
                } else {
                    incorrectConcepts.add("Question about: ${question.question}")
                }
            }
            
            val result = geminiRepository.generateFeedback(
                topic = data.topic,
                score = score,
                totalQuestions = data.questions.size,
                incorrectConcepts = incorrectConcepts
            )
            
            result.onSuccess { feedback ->
                _feedbackData.value = feedback
                _quizState.value = QuizState.RESULTS
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to generate feedback."
                _quizState.value = QuizState.ERROR
            }
        }
    }

    fun checkAnswer(question: Question, userAnswer: String): Boolean {
        if (userAnswer.isBlank()) return false
        val isCorrect = if (question.type == "single_choice" || question.type == "true_false") {
            question.correct_answer.trim().equals(userAnswer.trim(), ignoreCase = true)
        } else {
            // Strict match for short answer after normalizing
            val normalizedCorrect = question.correct_answer.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
            val normalizedUser = userAnswer.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
            normalizedCorrect == normalizedUser
        }
        android.util.Log.d("QuizScoring", "Q: ${question.question.take(20)}... | Expected: '${question.correct_answer}' | Selected: '$userAnswer' | Match: $isCorrect")
        return isCorrect
    }

    fun getScore(): Int {
        val data = _quizData.value ?: return 0
        var score = 0
        data.questions.forEachIndexed { index, question ->
            if (checkAnswer(question, _userAnswers.value[index] ?: "")) score++
        }
        return score
    }

    fun getTimeTakenSeconds(): Long {
        return (quizEndTime - quizStartTime) / 1000
    }
    
    fun saveAttempt(userId: String) {
        android.util.Log.d("QuizViewModel", "saveAttempt called for userId: $userId")
        viewModelScope.launch {
            val data = _quizData.value
            if (data == null) {
                android.util.Log.e("QuizViewModel", "saveAttempt failed: _quizData is null")
                return@launch
            }
            val score = getScore()
            val total = data.questions.size
            val accuracy = if (total > 0) ((score.toFloat() / total) * 100).toInt().coerceIn(0, 100) else 0
            android.util.Log.d("QuizViewModel", "Saving attempt: score=$score, accuracy=$accuracy")
            
            val questionHistories = data.questions.mapIndexed { index, q ->
                val userAnswer = _userAnswers.value[index] ?: ""
                QuestionHistory(
                    questionText = q.question,
                    userAnswer = userAnswer,
                    correctAnswer = q.correct_answer,
                    isCorrect = checkAnswer(q, userAnswer)
                )
            }
            
            val format = if (data.questions.isNotEmpty()) data.questions[0].type else "unknown"

            val history = QuizHistoryEntity(
                topic = data.topic,
                format = format,
                questionCount = total,
                score = score,
                accuracy = accuracy,
                questionsJson = jsonAdapter.toJson(questionHistories)
            )
            try {
                historyDao.insertQuizHistory(history)
                android.util.Log.d("QuizViewModel", "Successfully inserted history into DB")
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Failed to insert history: ${e.message}", e)
            }
        }
    }
    
    fun reset() {
        _quizState.value = QuizState.IDLE
        _quizData.value = null
        _feedbackData.value = null
        _errorMessage.value = null
    }
}
