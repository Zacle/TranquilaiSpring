package com.tranquilai.plan.dto.request

data class GeneratePlanRequest(
    val languageCode: String = "en",
    /** Force a new AI generation even if a plan already exists for today */
    val forceRegenerate: Boolean = false,
)
