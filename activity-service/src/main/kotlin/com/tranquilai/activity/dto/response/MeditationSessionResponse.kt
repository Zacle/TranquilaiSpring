package com.tranquilai.activity.dto.response

import java.util.UUID

data class MeditationSessionResponse(
    val id: UUID,
    val userId: UUID,
    val topicId: String,
    val meditationTitle: String,
    val durationSeconds: Int,
    val actualDurationSeconds: Int,
    val completedAt: Long,
    val feelingRating: Int?,
    val soundsUsed: List<String>,
    val createdAt: Long,
)
