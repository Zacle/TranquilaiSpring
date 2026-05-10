package com.tranquilai.activity.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class LogMoodRequest(
    @field:NotNull @field:Min(1) @field:Max(10)
    val moodScore: Int,

    val moodLabel: String? = null,
    val notes: String? = null,
    val factors: List<String> = emptyList(),
    val emotions: List<String> = emptyList(),
    val stressCauses: List<String> = emptyList(),
    val languageCode: String = "en",
)
