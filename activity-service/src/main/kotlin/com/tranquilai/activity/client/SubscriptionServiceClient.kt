package com.tranquilai.activity.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class SubscriptionServiceClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.subscription-service-url}") private val subscriptionServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(SubscriptionServiceClient::class.java)

    fun checkEntitlement(userId: UUID, feature: String): EntitlementResponse {
        return try {
            restTemplate.exchange(
                "$subscriptionServiceUrl/internal/subscriptions/entitlement?userId=$userId&feature=$feature",
                HttpMethod.GET,
                HttpEntity(null, internalHeaders()),
                EntitlementResponse::class.java,
            ).body ?: EntitlementResponse(allowed = false, plan = "UNKNOWN")
        } catch (ex: Exception) {
            logger.warn("Subscription entitlement check failed for user $userId feature=$feature: ${ex.message}")
            EntitlementResponse(allowed = false, plan = "UNKNOWN")
        }
    }

    private fun internalHeaders() = HttpHeaders().apply {
        set("X-Internal-Key", internalKey)
        contentType = MediaType.APPLICATION_JSON
    }
}

data class EntitlementResponse(
    val allowed: Boolean = false,
    val remaining: Int? = null,
    val plan: String = "UNKNOWN",
)
