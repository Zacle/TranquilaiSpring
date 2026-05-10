package com.tranquilai.ai.dto.response

data class JournalSummaryResponse(
    val summary: String,
    val keyThemes: List<String>,
    val emotionalTone: String,
    val suggestedFollowUp: String?,
)

data class MoodInsightResponse(
    val insight: String,
)

data class AffirmationResponse(
    val affirmation: String,
)

data class GreetingResponse(
    val greeting: String,
)

data class ConversationAnalysisResponse(
    val conversationId: String,
    val title: String,
    val summary: String,
    val keyTopics: List<String>,
    val moodAtStart: String?,
    val moodAtEnd: String?,
)
