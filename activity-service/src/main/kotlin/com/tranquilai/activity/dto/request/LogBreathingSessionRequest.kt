package com.tranquilai.activity.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class LogBreathingSessionRequest(
    @field:NotBlank
    val exerciseId: String,

    @field:NotBlank
    val exerciseTitle: String,

    @field:Min(1)
    val selectedDurationSeconds: Int,

    @field:Min(0)
    val actualDurationSeconds: Int,

    @field:Min(0)
    val completedCycles: Int,

    val completedAt: Long,

    @field:Min(1) @field:Max(5)
    val feelingRating: Int? = null,
)
