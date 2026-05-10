package com.tranquilai.activity.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateMoodRequest(
    @field:Min(1) @field:Max(10)
    val moodScore: Int? = null,

    val moodLabel: String? = null,
    val notes: String? = null,
    val factors: List<String>? = null,
)
