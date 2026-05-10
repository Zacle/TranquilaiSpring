package com.tranquilai.plan.dto.response

import java.util.UUID

data class PlanActivityResponse(
    val id: UUID,
    /** MOOD_TRACKING | CHAT_WITH_AI | JOURNALING | BREATHING_EXERCISE | MEDITATION | AFFIRMATION */
    val type: String,
    val title: String,
    val description: String?,
    /** Contextual prompt for journaling, affirmation, or reflection */
    val prompt: String?,
    val durationMinutes: Int,
    val order: Int,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val completionRating: Int?,
    val completionNotes: String?,
)

data class DailyPlanResponse(
    val id: UUID,
    val userId: UUID,
    val date: Long,
    val greeting: String,
    val motivationalMessage: String,
    val totalDurationMinutes: Int,
    val completedActivities: Int,
    val totalActivities: Int,
    val progressPercentage: Float,
    val isFullyCompleted: Boolean,
    val nextActivity: PlanActivityResponse?,
    val activities: List<PlanActivityResponse>,
    val aiGeneratedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
