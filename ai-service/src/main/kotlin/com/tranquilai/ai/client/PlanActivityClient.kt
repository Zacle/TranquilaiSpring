package com.tranquilai.ai.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PlanActivityClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.plan-service-url}") private val planServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(PlanActivityClient::class.java)

    fun completeByType(userId: String, activityType: String) {
        try {
            restTemplate.exchange(
                "$planServiceUrl/internal/plans/$userId/complete-by-type?type=$activityType",
                HttpMethod.POST,
                HttpEntity(null, internalHeaders()),
                Void::class.java,
            )
        } catch (ex: Exception) {
            logger.warn("Failed to complete plan activity type=$activityType for user $userId: ${ex.message}")
            throw ex
        }
    }

    private fun internalHeaders() = HttpHeaders().apply {
        set("X-Internal-Key", internalKey)
        contentType = MediaType.APPLICATION_JSON
    }
}
