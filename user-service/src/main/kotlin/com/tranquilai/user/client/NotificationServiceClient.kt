package com.tranquilai.user.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.UUID

/**
 * Syncs reminder settings to notification-service whenever a user updates them.
 * Uses X-Internal-Key for service-to-service auth.
 */
@Component
class NotificationServiceClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.notification-service-url:http://localhost:8088}") private val notificationServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(NotificationServiceClient::class.java)

    fun upsertReminderSchedule(
        userId: UUID,
        enabled: Boolean,
        reminderTimes: List<String>,
        frequency: String,
    ) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Internal-Key", internalKey)
        }
        val body = mapOf(
            "enabled" to enabled,
            "reminderTimes" to reminderTimes,
            "frequency" to frequency,
        )
        runCatching {
            restTemplate.exchange(
                "$notificationServiceUrl/internal/notifications/reminder-schedules/$userId",
                HttpMethod.PUT,
                HttpEntity(body, headers),
                Any::class.java,
            )
        }.onFailure { ex ->
            // Log but don't fail — reminder sync is best-effort
            logger.warn("Failed to sync reminder schedule for user $userId to notification-service: ${ex.message}")
        }
    }
}
