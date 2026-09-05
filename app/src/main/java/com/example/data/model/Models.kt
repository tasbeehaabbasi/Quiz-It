package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Question(
    val question: String,
    val type: String, // "single_choice", "true_false", "short_answer"
    val options: List<String>? = null,
    val correct_answer: String
)

@JsonClass(generateAdapter = true)
data class QuizGenerationResponse(
    val topic: String,
    val questions: List<Question>
)

@JsonClass(generateAdapter = true)
data class QuizFeedbackResponse(
    val mastery_message: String,
    val feedback_text: String,
    val weak_concept_title: String,
    val weak_concept_subtext: String,
    val study_tip: String
)

// Gemini API models (using Moshi)
@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val type: String, // e.g. "JSON_OBJECT"
    val schema: Map<String, Any>? = null // This would need a custom adapter or map, but we can rely on standard JSON format prompt if schema is complex to model. Actually we can just use "responseMimeType" in Gemini.
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)
