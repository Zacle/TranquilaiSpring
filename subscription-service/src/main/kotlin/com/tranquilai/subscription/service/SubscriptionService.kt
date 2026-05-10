package com.tranquilai.subscription.service

import com.tranquilai.subscription.config.StripeConfig
import com.tranquilai.subscription.dto.request.CreateCheckoutRequest
import com.tranquilai.subscription.dto.request.VerifyPlayPurchaseRequest
import com.tranquilai.subscription.dto.response.*
import com.tranquilai.subscription.entity.*
import com.tranquilai.subscription.exception.ResourceNotFoundException
import com.tranquilai.subscription.exception.SubscriptionException
import com.tranquilai.subscription.repository.InvoiceRepository
import com.tranquilai.subscription.repository.SubscriptionRepository
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
    private val stripeService: StripeService,
    private val playBillingService: PlayBillingService,
    private val subscriptionCacheService: SubscriptionCacheService,
    private val stripeConfig: StripeConfig,
    @param:Value("\${app.trial-days}") private val trialDays: Long,
) {
    private val logger = LoggerFactory.getLogger(SubscriptionService::class.java)

    @Transactional
    fun getOrCreateFreeSubscription(userId: UUID): Subscription {
        return subscriptionRepository.findByUserId(userId).orElseGet {
            subscriptionRepository.save(
                Subscription(userId = userId),
            )
        }
    }

    @Transactional
    fun getCurrentSubscription(userId: UUID): SubscriptionResponse {
        val sub = getOrCreateFreeSubscription(userId)
        return sub.toResponse()
    }

    fun getAvailablePlans(): List<PlanResponse> = listOf(
        PlanResponse(
            id = "premium_monthly",
            name = "TranquilAI Premium Monthly",
            priceId = stripeConfig.premiumMonthlyPriceId,
            amountCents = 999,
            currency = "USD",
            interval = "month",
            trialDays = trialDays.toInt(),
        ),
        PlanResponse(
            id = "premium_annual",
            name = "TranquilAI Premium Annual",
            priceId = stripeConfig.premiumAnnualPriceId,
            amountCents = 7999,
            currency = "USD",
            interval = "year",
            trialDays = trialDays.toInt(),
        ),
    )

    @Transactional
    fun createCheckoutSession(userId: UUID, userEmail: String, request: CreateCheckoutRequest): CheckoutResponse {
        if (request.priceId !in setOf(stripeConfig.premiumMonthlyPriceId, stripeConfig.premiumAnnualPriceId)) {
            throw SubscriptionException("Unknown subscription price")
        }

        val existingSub = getOrCreateFreeSubscription(userId)
        if (existingSub.isPremium()) {
            throw SubscriptionException(
                when (existingSub.paymentProvider) {
                    PaymentProvider.GOOGLE_PLAY -> "User already has an active Google Play subscription"
                    PaymentProvider.STRIPE -> "User already has an active Stripe subscription"
                    null -> "User already has an active subscription"
                },
            )
        }

        val customerId = ensureStripeCustomer(existingSub, userEmail)

        val session = stripeService.createCheckoutSession(
            customerId = customerId,
            priceId = request.priceId,
            successUrl = request.successUrl,
            cancelUrl = request.cancelUrl,
            trialDays = trialDays,
        )

        return CheckoutResponse(checkoutUrl = session.url, sessionId = session.id)
    }

    @Transactional
    fun getBillingPortalUrl(userId: UUID, userEmail: String): BillingPortalResponse {
        val sub = getOrCreateFreeSubscription(userId)
        return when (sub.paymentProvider) {
            PaymentProvider.GOOGLE_PLAY -> BillingPortalResponse(
                paymentProvider = PaymentProvider.GOOGLE_PLAY.name,
                externalManagementMessage = "Manage this subscription in the Google Play Store",
            )
            PaymentProvider.STRIPE -> {
                val customerId = ensureStripeCustomer(sub, userEmail)
                val portalSession = stripeService.createBillingPortalSession(customerId, stripeConfig.billingPortalReturnUrl)
                BillingPortalResponse(
                    paymentProvider = PaymentProvider.STRIPE.name,
                    portalUrl = portalSession.url,
                )
            }
            null -> BillingPortalResponse(
                paymentProvider = "NONE",
                externalManagementMessage = "You are on the free plan. Upgrade to manage billing.",
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
        if (sub.paymentProvider == PaymentProvider.GOOGLE_PLAY) {
            throw SubscriptionException("Google Play subscriptions must be canceled in the Google Play Store")
        }

        if (sub.stripeSubscriptionId != null) {
            stripeService.cancelAtPeriodEnd(sub.stripeSubscriptionId!!)
        }

        sub.cancelAtPeriodEnd = true
        sub.updatedAt = Instant.now()
        val updated = subscriptionRepository.save(sub)
        subscriptionCacheService.evictUser(userId)
        return updated.toResponse()
    }

    @Transactional
    fun reactivateSubscription(userId: UUID): SubscriptionResponse {
        val sub = subscriptionRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Subscription not found") }

        if (!sub.cancelAtPeriodEnd) throw SubscriptionException("Subscription is not set to cancel")
        if (sub.status !in setOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING)) {
            throw SubscriptionException("Cannot reactivate a subscription that is no longer active")
        }
        if (sub.paymentProvider == PaymentProvider.GOOGLE_PLAY) {
            throw SubscriptionException("Google Play subscriptions must be reactivated in the Google Play Store")
        }

        if (sub.stripeSubscriptionId != null) {
            stripeService.reactivate(sub.stripeSubscriptionId!!)
        }

        sub.cancelAtPeriodEnd = false
        sub.updatedAt = Instant.now()
        val updated = subscriptionRepository.save(sub)
        subscriptionCacheService.evictUser(userId)
        return updated.toResponse()
    }

    @Transactional(readOnly = true)
    fun getInvoices(userId: UUID): List<InvoiceResponse> =
        invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId).map { it.toResponse() }

    // ── Stripe webhook handlers ────────────────────────────────────────────────

    @Transactional
    fun handleSubscriptionActivated(
        userId: UUID,
        stripeSubscriptionId: String,
        stripeCustomerId: String,
        priceId: String,
        periodStart: Instant,
        periodEnd: Instant,
        trialEnd: Instant?,
        isTrialing: Boolean,
        cancelAtPeriodEnd: Boolean,
    ) {
        val sub = subscriptionRepository.findByUserId(userId).orElseGet {
            Subscription(userId = userId)
        }
        sub.stripeSubscriptionId = stripeSubscriptionId
        sub.stripeCustomerId = stripeCustomerId
        sub.planType = stripeService.priceIdToPlanType(priceId)
        sub.status = if (isTrialing) SubscriptionStatus.TRIALING else SubscriptionStatus.ACTIVE
        sub.paymentProvider = PaymentProvider.STRIPE
        sub.currentPeriodStart = periodStart
        sub.currentPeriodEnd = periodEnd
        sub.trialEnd = trialEnd
        sub.cancelAtPeriodEnd = cancelAtPeriodEnd
        sub.updatedAt = Instant.now()
        subscriptionRepository.save(sub)
        subscriptionCacheService.evictUser(userId)
        logger.info("Activated subscription for user {}: plan={}", userId, sub.planType)
    }

    @Transactional
    fun handleSubscriptionRenewed(
        userId: UUID,
        stripeSubscriptionId: String,
        periodStart: Instant,
        periodEnd: Instant,
    ) {
        val sub = subscriptionRepository.findByUserId(userId).orElse(null) ?: return
        sub.status = SubscriptionStatus.ACTIVE
        sub.currentPeriodStart = periodStart
        sub.currentPeriodEnd = periodEnd
        sub.updatedAt = Instant.now()
        subscriptionRepository.save(sub)
        subscriptionCacheService.evictUser(userId)
    }

    @Transactional
    fun handleSubscriptionCanceled(userId: UUID, stripeSubscriptionId: String) {
        val sub = subscriptionRepository.findByUserId(userId).orElse(null) ?: return
        sub.status = SubscriptionStatus.CANCELED
        sub.updatedAt = Instant.now()
        subscriptionRepository.save(sub)
        subscriptionCacheService.evictUser(userId)
        logger.info("Canceled subscription for user {}", userId)
    }

    @Transactional
    fun handlePaymentFailed(userId: UUID) {
        val sub = subscriptionRepository.findByUserId(userId).orElse(null) ?: return
        sub.status = SubscriptionStatus.PAST_DUE
        sub.updatedAt = Instant.now()
        subscriptionRepository.save(sub)
        subscriptionCacheService.evictUser(userId)
    }

    @Transactional
    fun recordInvoicePaid(
        userId: UUID,
        subscriptionId: UUID?,
        stripeInvoiceId: String,
        amountCents: Int,
        currency: String,
    ) {
        if (invoiceRepository.findByStripeInvoiceId(stripeInvoiceId).isPresent) return // idempotent
        invoiceRepository.save(
            Invoice(
                subscriptionId = subscriptionId,
                userId = userId,
                stripeInvoiceId = stripeInvoiceId,
                amountCents = amountCents,
                currency = currency,
                status = InvoiceStatus.PAID,
                paymentProvider = PaymentProvider.STRIPE,
                paidAt = Instant.now(),
            ),
        )
    }

    @Transactional
    fun recordInvoiceFailed(
        userId: UUID,
        subscriptionId: UUID?,
        stripeInvoiceId: String,
        amountCents: Int,
        currency: String,
    ) {
        if (invoiceRepository.findByStripeInvoiceId(stripeInvoiceId).isPresent) return
        invoiceRepository.save(
            Invoice(
                subscriptionId = subscriptionId,
                userId = userId,
                stripeInvoiceId = stripeInvoiceId,
                amountCents = amountCents,
                currency = currency,
                status = InvoiceStatus.FAILED,
                paymentProvider = PaymentProvider.STRIPE,
            ),
        )
    }

    // ── Google Play ──────────────────────────────────────────────────────────

    @Transactional
    fun verifyAndActivatePlayPurchase(userId: UUID, request: VerifyPlayPurchaseRequest): SubscriptionResponse {
        val verifiedPurchase = playBillingService.verifySubscription(request)
        val sub = getOrCreateFreeSubscription(userId)
        if (sub.isPremium()) {
            throw SubscriptionException(
                when (sub.paymentProvider) {
                    PaymentProvider.STRIPE -> "User already has an active Stripe subscription"
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

    // ── Internal helpers ─────────────────────────────────────────────────────

    fun findUserIdByStripeCustomerId(stripeCustomerId: String): UUID? =
        subscriptionRepository.findByStripeCustomerId(stripeCustomerId).orElse(null)?.userId

    fun findSubscriptionIdByUserId(userId: UUID): UUID? =
        subscriptionRepository.findByUserId(userId).orElse(null)?.id

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

    private fun ensureStripeCustomer(subscription: Subscription, userEmail: String): String {
        subscription.stripeCustomerId?.let { return it }

        val customerId = stripeService.createCustomer(userEmail, subscription.userId.toString())
        subscription.stripeCustomerId = customerId
        subscription.updatedAt = Instant.now()
        subscriptionRepository.save(subscription)
        return customerId
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
}
