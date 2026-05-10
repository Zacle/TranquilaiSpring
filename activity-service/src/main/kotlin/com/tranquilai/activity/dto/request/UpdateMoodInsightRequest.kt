package com.tranquilai.activity.dto.request

import jakarta.validation.constraints.NotBlank

data class UpdateMoodInsightRequest(
    @field:NotBlank(message = "Insight content is required")
    val insight: String,
)
