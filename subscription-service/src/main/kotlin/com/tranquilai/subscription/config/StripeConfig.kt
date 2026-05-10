package com.tranquilai.subscription.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class StripeConfig(
    @param:Value("\${stripe.secret-key}") val secretKey: String,
    @param:Value("\${stripe.webhook-secret}") val webhookSecret: String,
    @param:Value("\${stripe.premium-monthly-price-id}") val premiumMonthlyPriceId: String,
    @param:Value("\${stripe.premium-annual-price-id}") val premiumAnnualPriceId: String,
    @param:Value("\${stripe.billing-portal-return-url}") val billingPortalReturnUrl: String,
) {
    @PostConstruct
    fun init() {
        Stripe.apiKey = secretKey
    }
}
