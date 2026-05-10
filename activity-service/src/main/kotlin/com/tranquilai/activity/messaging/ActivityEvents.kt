package com.tranquilai.activity.messaging

import java.util.UUID

data class ProgressStatsEvent(
    val userId: UUID,
    val sessionsIncrement: Int = 0,
    val minutesIncrement: Int = 0,
    val moodEntriesIncrement: Int = 0,
    val journalEntriesIncrement: Int = 0,
    val markDayActive: Boolean = false,
)

data class PlanActivityCompletedEvent(
    val userId: UUID,
    val activityType: String,
)

data class MoodInsightRequestedEvent(
    val entryId: UUID,
    val moodScore: Int,
    val emotions: List<String> = emptyList(),
    val notes: String? = null,
    val stressCauses: List<String> = emptyList(),
    val languageCode: String = "en",
)

data class MoodInsightGeneratedEvent(
    val entryId: UUID,
    val insight: String,
)

data class JournalSummaryRequestedEvent(
    val entryId: UUID,
    val promptText: String = "",
    val content: String,
    val category: String? = null,
    val languageCode: String = "en",
)

data class JournalSummaryGeneratedEvent(
    val entryId: UUID,
    val summary: String,
    val keyThemes: List<String> = emptyList(),
    val emotionalTone: String,
)
