package com.tranquilai.subscription.dto.request

import jakarta.validation.constraints.NotBlank

data class CreateCheckoutRequest(
    @field:NotBlank val priceId: String,
    val successUrl: String = "https://tranquilai.cloud/subscription/success",
    val cancelUrl: String = "https://tranquilai.cloud/subscription/cancel",
)

data class VerifyPlayPurchaseRequest(
    @field:NotBlank val purchaseToken: String,
    @field:NotBlank val productId: String,
)

data class IncrementUsageRequest(
    @field:NotBlank val feature: String,
)
