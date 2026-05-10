package com.tranquilai.plan.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class CompleteActivityRequest(
    @field:Min(1) @field:Max(5)
    val rating: Int? = null,
    val notes: String? = null,
)
