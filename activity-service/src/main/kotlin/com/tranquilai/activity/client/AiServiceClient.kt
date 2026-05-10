package com.tranquilai.activity.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AiServiceClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.ai-service-url}") private val aiServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(AiServiceClient::class.java)

    fun generateMoodInsight(request: MoodInsightClientRequest): String? {
        return try {
            val response = restTemplate.exchange(
                "$aiServiceUrl/internal/insights/mood",
                HttpMethod.POST,
                HttpEntity(request, internalHeaders()),
                MoodInsightClientResponse::class.java,
            )
            response.body?.insight
        } catch (ex: Exception) {
            logger.warn("AI insight generation failed: ${ex.message}")
            throw ex
        }
    }

    fun summarizeJournal(request: JournalSummaryClientRequest): JournalSummaryClientResponse? {
        return try {
            val response = restTemplate.exchange(
                "$aiServiceUrl/internal/insights/journal",
                HttpMethod.POST,
                HttpEntity(request, internalHeaders()),
                JournalSummaryClientResponse::class.java,
            )
            response.body
        } catch (ex: Exception) {
            logger.warn("Journal summarization failed: ${ex.message}")
            throw ex
        }
    }

    private fun internalHeaders() = HttpHeaders().apply {
        set("X-Internal-Key", internalKey)
        contentType = MediaType.APPLICATION_JSON
    }
}

data class MoodInsightClientRequest(
    val moodScore: Int,
    val emotions: List<String>,
    val notes: String?,
    val stressCauses: List<String>,
    val languageCode: String,
)

data class MoodInsightClientResponse(val insight: String)

data class JournalSummaryClientRequest(
    val promptText: String,
    val content: String,
    val category: String?,
    val languageCode: String,
)

data class JournalSummaryClientResponse(
    val summary: String,
    val keyThemes: List<String>,
    val emotionalTone: String,
    val suggestedFollowUp: String?,
)
