package com.tranquilai.subscription.service

import com.tranquilai.subscription.dto.request.VerifyPlayPurchaseRequest
import com.tranquilai.subscription.dto.response.BillingPortalResponse
import com.tranquilai.subscription.dto.response.InvoiceResponse
import com.tranquilai.subscription.dto.response.PlanResponse
import com.tranquilai.subscription.dto.response.SubscriptionResponse
import com.tranquilai.subscription.entity.Invoice
import com.tranquilai.subscription.entity.PaymentProvider
import com.tranquilai.subscription.entity.PlanType
import com.tranquilai.subscription.entity.Subscription
import com.tranquilai.subscription.entity.SubscriptionStatus
import com.tranquilai.subscription.exception.ResourceNotFoundException
import com.tranquilai.subscription.exception.SubscriptionException
import com.tranquilai.subscription.repository.EntitlementGrantRepository
import com.tranquilai.subscription.repository.InvoiceRepository
import com.tranquilai.subscription.repository.SubscriptionRepository
import com.tranquilai.subscription.repository.UsageRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val invoiceRepository: InvoiceRepository,
    private val usageRecordRepository: UsageRecordRepository,
    private val entitlementGrantRepository: EntitlementGrantRepository,
    private val playBillingService: PlayBillingService,
    private val subscriptionCacheService: SubscriptionCacheService,
    @param:Value("\${app.trial-days}") private val trialDays: Long,
) {
    private val logger = LoggerFactory.getLogger(SubscriptionService::class.java)

    @Transactional
    fun getOrCreateFreeSubscription(userId: UUID): Subscription {
        return subscriptionRepository.findByUserId(userId).orElseGet {
            subscriptionRepository.save(Subscription(userId = userId))
        }
    }

    @Transactional
    fun getCurrentSubscription(userId: UUID): SubscriptionResponse =
        getOrCreateFreeSubscription(userId).toResponse()

    fun getAvailablePlans(): List<PlanResponse> = listOf(
        PlanResponse(
            id = "premium_monthly",
            name = "TranquilAI Premium Monthly",
            priceId = GOOGLE_PLAY_MONTHLY_PRODUCT_ID,
            amountCents = 299,
            currency = "USD",
            interval = "month",
            trialDays = trialDays.toInt(),
        ),
        PlanResponse(
            id = "premium_annual",
            name = "TranquilAI Premium Annual",
            priceId = GOOGLE_PLAY_ANNUAL_PRODUCT_ID,
            amountCents = 2399,
            currency = "USD",
            interval = "year",
            trialDays = trialDays.toInt(),
        ),
    )

    @Transactional
    fun getBillingPortalUrl(userId: UUID, userEmail: String): BillingPortalResponse {
        val sub = getOrCreateFreeSubscription(userId)
        return when (sub.paymentProvider) {
            PaymentProvider.GOOGLE_PLAY -> BillingPortalResponse(
                paymentProvider = PaymentProvider.GOOGLE_PLAY.name,
                externalManagementMessage = "Manage this subscription in the Google Play Store",
            )
            null -> BillingPortalResponse(
                paymentProvider = "NONE",
                externalManagementMessage = "You are on the free plan. Upgrade from the mobile app with Google Play.",
            )
        }
    }

    @Transactional
    fun cancelSubscription(userId: UUID): SubscriptionResponse {
        val sub = subscriptionRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Subscription not found") }

        if (
            sub.planType == PlanType.FREE ||
            sub.status == SubscriptionStatus.CANCELED ||
            sub.status == SubscriptionStatus.EXPIRED
        ) {
            throw SubscriptionException("No active paid subscription to cancel")
        }

        throw SubscriptionException("Mobile subscriptions must be canceled in the Google Play Store")
    }

    @Transactional
    fun reactivateSubscription(userId: UUID): SubscriptionResponse {
        val sub = subscriptionRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Subscription not found") }

        if (!sub.cancelAtPeriodEnd) throw SubscriptionException("Subscription is not set to cancel")
        if (sub.status !in setOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING)) {
            throw SubscriptionException("Cannot reactivate a subscription that is no longer active")
        }

        throw SubscriptionException("Mobile subscriptions must be reactivated in the Google Play Store")
    }

    @Transactional(readOnly = true)
    fun getInvoices(userId: UUID): List<InvoiceResponse> =
        invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId).map { it.toResponse() }

    @Transactional
    fun verifyAndActivatePlayPurchase(userId: UUID, request: VerifyPlayPurchaseRequest): SubscriptionResponse {
        val verifiedPurchase = playBillingService.verifySubscription(request)
        val sub = getOrCreateFreeSubscription(userId)
        if (sub.isPremium()) {
            throw SubscriptionException(
                when (sub.paymentProvider) {
                    PaymentProvider.GOOGLE_PLAY -> "User already has an active Google Play subscription"
                    null -> "User already has an active subscription"
                },
            )
        }
        sub.googlePlayPurchaseToken = request.purchaseToken
        sub.planType = verifiedPurchase.planType
        sub.status = SubscriptionStatus.ACTIVE
        sub.paymentProvider = PaymentProvider.GOOGLE_PLAY
        sub.currentPeriodStart = verifiedPurchase.startsAt
        sub.currentPeriodEnd = verifiedPurchase.expiresAt
        sub.cancelAtPeriodEnd = !verifiedPurchase.autoRenewing
        sub.updatedAt = Instant.now()
        val updated = subscriptionRepository.save(sub)
        subscriptionCacheService.evictUser(userId)
        return updated.toResponse()
    }

    @Transactional
    fun handleGooglePlayNotification(productId: String, purchaseToken: String, notificationType: Int) {
        val sub = subscriptionRepository.findByGooglePlayPurchaseToken(purchaseToken).orElse(null) ?: run {
            logger.warn("No subscription found for Google Play purchase token notification")
            return
        }

        when (notificationType) {
            1, 2, 7 -> {
                val verifiedPurchase = playBillingService.verifySubscription(
                    VerifyPlayPurchaseRequest(purchaseToken = purchaseToken, productId = productId),
                )
                sub.planType = verifiedPurchase.planType
                sub.status = SubscriptionStatus.ACTIVE
                sub.currentPeriodStart = verifiedPurchase.startsAt
                sub.currentPeriodEnd = verifiedPurchase.expiresAt
                sub.cancelAtPeriodEnd = !verifiedPurchase.autoRenewing
                sub.paymentProvider = PaymentProvider.GOOGLE_PLAY
            }
            3, 12 -> sub.cancelAtPeriodEnd = true
            4, 13 -> sub.status = SubscriptionStatus.EXPIRED
            5, 6 -> sub.status = SubscriptionStatus.PAST_DUE
            else -> logger.debug("Unhandled Google Play notification type: {}", notificationType)
        }

        sub.updatedAt = Instant.now()
        subscriptionRepository.save(sub)
        subscriptionCacheService.evictUser(sub.userId)
    }

    @Transactional
    fun deleteAccountSubscriptionData(userId: UUID) {
        val sub = subscriptionRepository.findByUserId(userId).orElse(null)
        if (sub?.paymentProvider == PaymentProvider.GOOGLE_PLAY && sub.googlePlayPurchaseToken != null) {
            productIdForPlan(sub.planType)?.let { productId ->
                runCatching {
                    playBillingService.cancelSubscription(
                        productId = productId,
                        purchaseToken = sub.googlePlayPurchaseToken!!,
                    )
                }.onFailure {
                    logger.warn("Failed to cancel Google Play subscription during account deletion for userId={}", userId)
                }
            }
        }

        usageRecordRepository.deleteByUserId(userId)
        entitlementGrantRepository.deleteByUserId(userId)
        invoiceRepository.deleteByUserId(userId)
        subscriptionRepository.deleteByUserId(userId)
        subscriptionCacheService.evictUser(userId)
    }

    private fun Subscription.toResponse() = SubscriptionResponse(
        id = id,
        userId = userId,
        planType = planType,
        status = status,
        isPremium = isPremium(),
        currentPeriodStart = currentPeriodStart,
        currentPeriodEnd = currentPeriodEnd,
        cancelAtPeriodEnd = cancelAtPeriodEnd,
        trialEnd = trialEnd,
        paymentProvider = paymentProvider?.name,
        createdAt = createdAt,
    )

    private fun Invoice.toResponse() = InvoiceResponse(
        id = id,
        amountCents = amountCents,
        currency = currency,
        status = status.name,
        paymentProvider = paymentProvider?.name,
        paidAt = paidAt,
        createdAt = createdAt,
    )

    private companion object {
        const val GOOGLE_PLAY_MONTHLY_PRODUCT_ID = "tranquilai_premium_monthly"
        const val GOOGLE_PLAY_ANNUAL_PRODUCT_ID = "tranquilai_premium_annual"
    }

    private fun productIdForPlan(planType: PlanType): String? =
        when (planType) {
            PlanType.PREMIUM_MONTHLY -> GOOGLE_PLAY_MONTHLY_PRODUCT_ID
            PlanType.PREMIUM_ANNUAL -> GOOGLE_PLAY_ANNUAL_PRODUCT_ID
            PlanType.FREE -> null
        }
}
