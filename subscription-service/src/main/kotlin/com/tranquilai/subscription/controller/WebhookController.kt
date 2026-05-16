package com.tranquilai.subscription.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.tranquilai.subscription.service.SubscriptionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
@RequestMapping("/api/webhooks")
class WebhookController(
    private val subscriptionService: SubscriptionService,
    private val objectMapper: ObjectMapper,
) {

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

    private fun ok() = ResponseEntity.ok(mapOf("received" to "true"))
}
