package com.tranquilai.subscription.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.tranquilai.subscription.config.StripeConfig
import com.tranquilai.subscription.service.SubscriptionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.http.HttpStatus
import java.util.Base64

class WebhookControllerTest {

    private val service: SubscriptionService = mock(SubscriptionService::class.java)
    private val controller = WebhookController(service, StripeConfig("sk", "whsec", "pm", "pa", "return"), ObjectMapper())

    @Test
    fun `google play webhook ignores messages without subscription notification`() {
        val encoded = Base64.getEncoder().encodeToString("""{"testNotification":{}}""".toByteArray())

        val response = controller.handleGooglePlayWebhook("""{"message":{"data":"$encoded"}}""")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(mapOf("received" to "true"), response.body)
    }

    @Test
    fun `google play webhook decodes notification and delegates`() {
        val notification = """
            {"subscriptionNotification":{"subscriptionId":"monthly","purchaseToken":"token","notificationType":4}}
        """.trimIndent()
        val encoded = Base64.getEncoder().encodeToString(notification.toByteArray())

        controller.handleGooglePlayWebhook("""{"message":{"data":"$encoded"}}""")

        verify(service).handleGooglePlayNotification("monthly", "token", 4)
    }
}
