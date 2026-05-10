package com.tranquilai.activity.dto.response

data class MoodDataPoint(
    val label: String,          // "Mon"/"Tue" | "1"/"2"... | "Jan"/"Feb"...
    val averageMoodScore: Float?, // null if no entries in this slot
    val entryCount: Int,
)

data class MoodChartResponse(
    val period: String,             // "week" | "month" | "year"
    val data: List<MoodDataPoint>,  // all slots, ordered chronologically
    val hasEnoughData: Boolean,
    val dataNeededCount: Int,       // 0 when hasEnoughData is true
)

data class ActivityBreakdownResponse(
    val period: String,
    val from: Long,
    val to: Long,
    val moodEntries: Long,
    val journalEntries: Long,
    val breathingSessions: Long,
    val meditationSessions: Long,
    val total: Long,
)
