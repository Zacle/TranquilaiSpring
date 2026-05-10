package com.tranquilai.ai.dto.request

import jakarta.validation.constraints.NotBlank

data class SummarizeJournalRequest(
    @field:NotBlank val promptText: String,
    @field:NotBlank val content: String,
    val category: String? = null,
    val stressCauses: List<String> = emptyList(),
    val currentConcerns: List<String> = emptyList(),
    val languageCode: String = "en",
)

data class MoodInsightRequest(
    val moodScore: Int,
    val emotions: List<String> = emptyList(),
    val notes: String? = null,
    val stressCauses: List<String> = emptyList(),
    val languageCode: String = "en",
)

data class GenerateAffirmationRequest(
    val stressCauses: List<String> = emptyList(),
    val currentConcerns: List<String> = emptyList(),
    val languageCode: String = "en",
)

data class GenerateGreetingRequest(
    val firstName: String = "there",
    val stressCauses: List<String> = emptyList(),
    val languageCode: String = "en",
)

data class AnalyzeConversationRequest(
    val conversationId: String,
)
