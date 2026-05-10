package com.tranquilai.activity.dto.request

import jakarta.validation.constraints.NotBlank

data class LogAffirmationViewRequest(
    @field:NotBlank val affirmationId: String,
)
