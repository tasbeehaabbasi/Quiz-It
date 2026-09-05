package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.Content
import com.example.data.model.GenerateContentRequest
import com.example.data.model.GenerationConfig
import com.example.data.model.Part
import com.example.data.model.QuizFeedbackResponse
import com.example.data.model.QuizGenerationResponse
import com.example.data.network.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun generateQuiz(
        sourceText: String,
        format: String,
        count: Int,
        difficulty: String
    ): Result<QuizGenerationResponse> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are an expert tutor. Generate a practice quiz based on the provided text.
                Format: $format
                Count: $count questions
                Difficulty: $difficulty
                
                Source text:
                $sourceText
                
                Return exactly $count questions matching the format and difficulty.
                For single_choice (MCQ): provide exactly 4 options, 1 correct_answer.
                For true_false: provide a clear true or false statement.
                For short_answer: provide a question and a specific short correct_answer.
                
                Respond ONLY with a valid JSON object matching this structure exactly (do not include markdown formatting or backticks):
                {
                  "topic": "short inferred topic label",
                  "questions": [
                    {
                      "question": "question text",
                      "type": "single_choice | true_false | short_answer",
                      "options": ["option 1", "option 2", "option 3", "option 4"], // only for single_choice or true_false
                      "correct_answer": "the exact correct answer string"
                    }
                  ]
                }
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No response from Gemini")
            
            // Clean up possible markdown code blocks if the model ignored the instruction
            val cleanedJson = jsonText.removePrefix("```json").removeSuffix("```").trim()

            val adapter = moshi.adapter(QuizGenerationResponse::class.java)
            val result = adapter.fromJson(cleanedJson) ?: throw Exception("Failed to parse JSON")
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateFeedback(
        topic: String,
        score: Int,
        totalQuestions: Int,
        incorrectConcepts: List<String>
    ): Result<QuizFeedbackResponse> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are an expert tutor giving feedback after a quiz.
                Topic: $topic
                Score: $score / $totalQuestions
                Incorrect Concepts: ${incorrectConcepts.joinToString(", ")}
                
                If the score is 100%, provide positive reinforcement for the weak_concept title and subtext.
                
                Respond ONLY with a valid JSON object matching this structure exactly (do not include markdown formatting or backticks):
                {
                  "mastery_message": "A short mastery message (e.g., Well done - 70% mastery)",
                  "feedback_text": "1-2 sentences referencing the topic and performance",
                  "weak_concept_title": "A single weak concept title derived from incorrect answers",
                  "weak_concept_subtext": "Short explanation of the weak concept",
                  "study_tip": "A general spaced-repetition/study tip"
                }
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No response from Gemini")

            val cleanedJson = jsonText.removePrefix("```json").removeSuffix("```").trim()

            val adapter = moshi.adapter(QuizFeedbackResponse::class.java)
            val result = adapter.fromJson(cleanedJson) ?: throw Exception("Failed to parse JSON")
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
