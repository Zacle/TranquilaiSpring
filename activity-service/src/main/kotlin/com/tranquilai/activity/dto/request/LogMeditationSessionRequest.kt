package com.tranquilai.activity.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class LogMeditationSessionRequest(
    @field:NotBlank
    val topicId: String,

    @field:NotBlank
    val meditationTitle: String,

    @field:Min(1)
    val durationSeconds: Int,

    @field:Min(0)
    val actualDurationSeconds: Int,

    val completedAt: Long,

    @field:Min(1) @field:Max(5)
    val feelingRating: Int? = null,

    val soundsUsed: List<String> = emptyList(),
)
