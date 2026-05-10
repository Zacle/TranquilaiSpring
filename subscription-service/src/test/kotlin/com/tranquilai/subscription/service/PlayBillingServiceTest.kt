package com.tranquilai.subscription.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tranquilai.subscription.dto.request.VerifyPlayPurchaseRequest
import com.tranquilai.subscription.exception.SubscriptionException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate

class PlayBillingServiceTest {

    @Test
    fun `verifySubscription fails fast when package name is missing`() {
        val service = PlayBillingService(RestTemplate(), ObjectMapper(), serviceAccountJson = "{}", packageName = "")

        assertThrows(SubscriptionException::class.java) {
            service.verifySubscription(VerifyPlayPurchaseRequest("token", "tranquilai_premium_monthly"))
        }
    }

    @Test
    fun `verifySubscription fails when service account is missing`() {
        val service = PlayBillingService(RestTemplate(), ObjectMapper(), serviceAccountJson = "", packageName = "com.tranquilai")

        assertThrows(SubscriptionException::class.java) {
            service.verifySubscription(VerifyPlayPurchaseRequest("token", "tranquilai_premium_monthly"))
        }
    }
}
