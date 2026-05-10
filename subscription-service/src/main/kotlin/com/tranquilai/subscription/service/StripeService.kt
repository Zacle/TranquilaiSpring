package com.tranquilai.subscription.service

import com.stripe.model.Customer
import com.stripe.model.Subscription
import com.stripe.net.RequestOptions
import com.stripe.model.billingportal.Session as PortalSession
import com.stripe.model.checkout.Session
import com.stripe.param.CustomerCreateParams
import com.stripe.param.billingportal.SessionCreateParams as PortalSessionCreateParams
import com.stripe.param.checkout.SessionCreateParams
import com.tranquilai.subscription.config.StripeConfig
import com.tranquilai.subscription.entity.PlanType
import com.tranquilai.subscription.exception.SubscriptionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StripeService(private val stripeConfig: StripeConfig) {

    private val logger = LoggerFactory.getLogger(StripeService::class.java)

    fun createCustomer(email: String, userId: String): String {
        val params = CustomerCreateParams.builder()
            .setEmail(email)
            .putMetadata("userId", userId)
            .build()
        val customer = Customer.create(params)
        logger.info("Created Stripe customer {} for user {}", customer.id, userId)
        return customer.id
    }

    fun createCheckoutSession(
        customerId: String,
        priceId: String,
        successUrl: String,
        cancelUrl: String,
        trialDays: Long,
    ): Session {
        val params = SessionCreateParams.builder()
            .setCustomer(customerId)
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPrice(priceId)
                    .setQuantity(1L)
                    .build(),
            )
            .setSubscriptionData(
                SessionCreateParams.SubscriptionData.builder()
                    .setTrialPeriodDays(trialDays)
                    .build(),
            )
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .build()

        val idempotencyKey = "checkout-$customerId-${priceId.takeLast(8)}"
        val requestOptions = RequestOptions.builder()
            .setIdempotencyKey(idempotencyKey)
            .build()
        return Session.create(params, requestOptions)
    }

    fun createBillingPortalSession(customerId: String, returnUrl: String): PortalSession {
        val params = PortalSessionCreateParams.builder()
            .setCustomer(customerId)
            .setReturnUrl(returnUrl)
            .build()
        return PortalSession.create(params)
    }

    fun cancelAtPeriodEnd(stripeSubscriptionId: String): Subscription {
        val subscription = Subscription.retrieve(stripeSubscriptionId)
        val params = com.stripe.param.SubscriptionUpdateParams.builder()
            .setCancelAtPeriodEnd(true)
            .build()
        return subscription.update(params)
    }

    fun reactivate(stripeSubscriptionId: String): Subscription {
        val subscription = Subscription.retrieve(stripeSubscriptionId)
        val params = com.stripe.param.SubscriptionUpdateParams.builder()
            .setCancelAtPeriodEnd(false)
            .build()
        return subscription.update(params)
    }

    fun priceIdToPlanType(priceId: String): PlanType =
        when (priceId) {
            stripeConfig.premiumMonthlyPriceId -> PlanType.PREMIUM_MONTHLY
            stripeConfig.premiumAnnualPriceId -> PlanType.PREMIUM_ANNUAL
            else -> throw SubscriptionException("Unrecognised Stripe price ID: $priceId")
        }
}
