package com.tranquilai.subscription.dto.request

import jakarta.validation.constraints.NotBlank

data class VerifyPlayPurchaseRequest(
    @field:NotBlank val purchaseToken: String,
    @field:NotBlank val productId: String,
)

data class IncrementUsageRequest(
    @field:NotBlank val feature: String,
)
