package com.tranquilai.subscription.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Invoice
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import com.tranquilai.subscription.config.StripeConfig
import com.tranquilai.subscription.exception.SubscriptionException
import com.tranquilai.subscription.exception.WebhookException
import com.tranquilai.subscription.service.SubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping("/api/webhooks")
class WebhookController(
    private val subscriptionService: SubscriptionService,
    private val stripeConfig: StripeConfig,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    /** POST /api/webhooks/stripe */
    @PostMapping("/stripe")
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") stripeSignature: String,
    ): ResponseEntity<Map<String, String>> {
        val event = try {
            Webhook.constructEvent(payload, stripeSignature, stripeConfig.webhookSecret)
        } catch (e: SignatureVerificationException) {
            logger.warn("Invalid Stripe webhook signature: {}", e.message)
            throw WebhookException("Invalid webhook signature")
        }

        logger.info("Received Stripe event: {}", event.type)

        // Process idempotently — log and ignore unknown event types
        when (event.type) {
            "customer.subscription.created",
            "customer.subscription.updated" -> {
                val subscription = event.dataObjectDeserializer.`object`.orElse(null) as? Subscription ?: return ok()
                try {
                    handleSubscriptionUpdated(subscription)
                } catch (ex: SubscriptionException) {
                    logger.error("Failed to handle subscription update: {}", ex.message)
                }
            }

            "customer.subscription.deleted" -> {
                val subscription = event.dataObjectDeserializer.`object`.orElse(null) as? Subscription ?: return ok()
                val userId = findUserId(subscription.customer) ?: return ok()
                subscriptionService.handleSubscriptionCanceled(userId, subscription.id)
            }

            "invoice.payment_succeeded" -> {
                val invoice = event.dataObjectDeserializer.`object`.orElse(null) as? Invoice ?: return ok()
                handleInvoicePaymentSucceeded(invoice)
            }

            "invoice.payment_failed" -> {
                val invoice = event.dataObjectDeserializer.`object`.orElse(null) as? Invoice ?: return ok()
                val userId = findUserId(invoice.customer) ?: return ok()
                subscriptionService.handlePaymentFailed(userId)
                subscriptionService.recordInvoiceFailed(
                    userId = userId,
                    subscriptionId = subscriptionService.findSubscriptionIdByUserId(userId),
                    stripeInvoiceId = invoice.id,
                    amountCents = invoice.amountDue.toInt(),
                    currency = invoice.currency.uppercase(),
                )
            }

            else -> logger.debug("Unhandled Stripe event type: {}", event.type)
        }

        return ok()
    }

    /** POST /api/webhooks/google-play */
    @PostMapping("/google-play")
    fun handleGooglePlayWebhook(@RequestBody payload: String): ResponseEntity<Map<String, String>> {
        val root = objectMapper.readTree(payload)
        val encodedData = root.path("message").path("data").asText(null) ?: return ok()
        val decodedData = String(Base64.getDecoder().decode(encodedData))
        val notification = objectMapper.readTree(decodedData).path("subscriptionNotification")
        if (notification.isMissingNode || notification.isNull) return ok()

        val productId = notification.path("subscriptionId").asText(null) ?: return ok()
        val purchaseToken = notification.path("purchaseToken").asText(null) ?: return ok()
        val notificationType = notification.path("notificationType").asInt(0)

        subscriptionService.handleGooglePlayNotification(productId, purchaseToken, notificationType)
        return ok()
    }

    private fun handleSubscriptionUpdated(subscription: Subscription) {
        val userId = findUserId(subscription.customer) ?: return
        val priceId = subscription.items?.data?.firstOrNull()?.price?.id ?: return
        val isTrialing = subscription.status == "trialing"
        val trialEnd = subscription.trialEnd?.let { Instant.ofEpochSecond(it) }

        subscriptionService.handleSubscriptionActivated(
            userId = userId,
            stripeSubscriptionId = subscription.id,
            stripeCustomerId = subscription.customer,
            priceId = priceId,
            periodStart = Instant.ofEpochSecond(subscription.currentPeriodStart),
            periodEnd = Instant.ofEpochSecond(subscription.currentPeriodEnd),
            trialEnd = trialEnd,
            isTrialing = isTrialing,
            cancelAtPeriodEnd = subscription.cancelAtPeriodEnd,
        )
    }

    private fun handleInvoicePaymentSucceeded(invoice: Invoice) {
        val userId = findUserId(invoice.customer) ?: return
        val subscriptionId = subscriptionService.findSubscriptionIdByUserId(userId)

        // Only record invoice for subscription invoices (not one-time charges)
        if (invoice.subscription != null) {
            val stripeSubscription = Subscription.retrieve(invoice.subscription)
            subscriptionService.handleSubscriptionRenewed(
                userId = userId,
                stripeSubscriptionId = invoice.subscription,
                periodStart = Instant.ofEpochSecond(stripeSubscription.currentPeriodStart),
                periodEnd = Instant.ofEpochSecond(stripeSubscription.currentPeriodEnd),
            )
        }

        subscriptionService.recordInvoicePaid(
            userId = userId,
            subscriptionId = subscriptionId,
            stripeInvoiceId = invoice.id,
            amountCents = invoice.amountPaid.toInt(),
            currency = invoice.currency.uppercase(),
        )
    }

    private fun findUserId(stripeCustomerId: String): UUID? {
        val userId = subscriptionService.findUserIdByStripeCustomerId(stripeCustomerId)
        if (userId == null) logger.warn("No user found for Stripe customer: {}", stripeCustomerId)
        return userId
    }

    private fun ok() = ResponseEntity.ok(mapOf("received" to "true"))
}
