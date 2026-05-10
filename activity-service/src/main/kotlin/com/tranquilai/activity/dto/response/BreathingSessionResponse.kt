package com.tranquilai.activity.dto.response

import java.util.UUID

data class BreathingSessionResponse(
    val id: UUID,
    val userId: UUID,
    val exerciseId: String,
    val exerciseTitle: String,
    val selectedDurationSeconds: Int,
    val actualDurationSeconds: Int,
    val completedCycles: Int,
    val completedAt: Long,
    val feelingRating: Int?,
    val createdAt: Long,
)
