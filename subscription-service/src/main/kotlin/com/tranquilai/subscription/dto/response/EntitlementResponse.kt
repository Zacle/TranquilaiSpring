package com.tranquilai.subscription.dto.response

data class EntitlementResponse(
    val allowed: Boolean,
    val remaining: Int? = null,
    val plan: String,
)

data class UsageResponse(
    val allowed: Boolean,
    val used: Int,
    val limit: Int?,
    val remaining: Int?,
    val plan: String,
)
