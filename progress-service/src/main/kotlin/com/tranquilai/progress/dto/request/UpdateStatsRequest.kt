package com.tranquilai.progress.dto.request

data class UpdateStatsRequest(
    val sessionsIncrement: Int = 0,
    val minutesIncrement: Int = 0,
    val moodEntriesIncrement: Int = 0,
    val journalEntriesIncrement: Int = 0,
    val chatSessionsIncrement: Int = 0,
    val plansCompletedIncrement: Int = 0,
    /** Provide the new streak value directly when streak changes */
    val streakDays: Int? = null,
    /** Set to true when this call represents a new active day */
    val markDayActive: Boolean = false,
    /** Updated rolling average mood score (0.0–10.0) */
    val averageMoodScore: Double? = null,
)
