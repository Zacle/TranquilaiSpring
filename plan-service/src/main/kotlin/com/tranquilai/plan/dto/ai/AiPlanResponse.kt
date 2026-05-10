package com.tranquilai.plan.dto.ai

/** Internal DTO used to parse the structured AI response */
data class AiPlanResponse(
    val greeting: String,
    val motivationalMessage: String,
    val activities: List<AiActivity>,
)

data class AiActivity(
    /** MOOD_TRACKING | CHAT_WITH_AI | JOURNALING | BREATHING_EXERCISE | MEDITATION | AFFIRMATION */
    val type: String,
    val title: String,
    val description: String,
    /** Contextual prompt for journaling / affirmation / reflection activities */
    val prompt: String? = null,
    val durationMinutes: Int,
)
