package com.tranquilai.activity.dto.response

import java.util.UUID

data class MoodEntryResponse(
    val id: UUID,
    val userId: UUID,
    val moodScore: Int,
    val moodLabel: String?,
    val notes: String?,
    val factors: String?,
    val aiInsight: String?,
    val createdAt: Long,
)
