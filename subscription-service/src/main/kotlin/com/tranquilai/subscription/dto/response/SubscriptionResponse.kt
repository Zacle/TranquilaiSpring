package com.tranquilai.subscription.dto.response

import com.tranquilai.subscription.entity.PlanType
import com.tranquilai.subscription.entity.SubscriptionStatus
import java.time.Instant
import java.util.UUID

data class SubscriptionResponse(
    val id: UUID,
    val userId: UUID,
    val planType: PlanType,
    val status: SubscriptionStatus,
    val isPremium: Boolean,
    val currentPeriodStart: Instant?,
    val currentPeriodEnd: Instant?,
    val cancelAtPeriodEnd: Boolean,
    val trialEnd: Instant?,
    val paymentProvider: String?,
    val createdAt: Instant,
)

data class CheckoutResponse(
    val checkoutUrl: String,
    val sessionId: String,
)

data class BillingPortalResponse(
    val paymentProvider: String,
    val portalUrl: String? = null,
    val externalManagementMessage: String? = null,
)

data class PlanResponse(
    val id: String,
    val name: String,
    val priceId: String,
    val amountCents: Int,
    val currency: String,
    val interval: String,
    val trialDays: Int,
)

data class InvoiceResponse(
    val id: UUID,
    val amountCents: Int,
    val currency: String,
    val status: String,
    val paymentProvider: String?,
    val paidAt: Instant?,
    val createdAt: Instant,
)
