package com.tranquilai.activity.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class ProgressServiceClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.progress-service-url}") private val progressServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(ProgressServiceClient::class.java)

    fun updateStats(userId: UUID, request: UpdateStatsClientRequest) {
        try {
            restTemplate.exchange(
                "$progressServiceUrl/internal/progress/stats/$userId",
                HttpMethod.PATCH,
                HttpEntity(request, internalHeaders()),
                Void::class.java,
            )
        } catch (ex: Exception) {
            logger.warn("Failed to update progress stats for user $userId: ${ex.message}")
            throw ex
        }
    }

    private fun internalHeaders() = HttpHeaders().apply {
        set("X-Internal-Key", internalKey)
        contentType = MediaType.APPLICATION_JSON
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
