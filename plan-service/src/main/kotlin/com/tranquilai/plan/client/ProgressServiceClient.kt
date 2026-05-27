package com.tranquilai.plan.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

@Component
class ProgressServiceClient(
    @param:Value("\${app.progress-service-url}") private val progressServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(ProgressServiceClient::class.java)

    private val restClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(progressServiceUrl)
            .defaultHeader("X-Internal-Key", internalKey)
            .build()
    }

    fun recordPlanCompleted(userId: UUID) {
        runCatching {
            restClient.patch()
                .uri("/internal/progress/stats/$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .body(UpdateStatsClientRequest(plansCompletedIncrement = 1, markDayActive = true))
                .retrieve()
                .toBodilessEntity()
        }.onFailure {
            logger.warn("Failed to record completed plan for user $userId: ${it.message}")
        }
    }
}

data class UpdateStatsClientRequest(
    val sessionsIncrement: Int = 0,
    val minutesIncrement: Int = 0,
    val moodEntriesIncrement: Int = 0,
    val journalEntriesIncrement: Int = 0,
    val chatSessionsIncrement: Int = 0,
    val plansCompletedIncrement: Int = 0,
    val markDayActive: Boolean = false,
    val streakDays: Int? = null,
    val averageMoodScore: Double? = null,
)
