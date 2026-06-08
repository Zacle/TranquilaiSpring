package com.tranquilai.user.client

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
    private fun headers() = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("X-Internal-Key", internalKey)
    }

    fun deleteSubscriptionData(userId: UUID) {
        restTemplate.exchange(
            "$subscriptionServiceUrl/internal/subscriptions/user/$userId",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers()),
            Void::class.java,
        )
    }
}
