package com.tranquilai.plan.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

data class PlanContextResponse(
    val userId: UUID,
    val firstName: String,
    val currentFeelingLevel: String?,
    val stressCauses: List<String> = emptyList(),
    val currentConcerns: List<String> = emptyList(),
    val mentalProcessPreferences: List<String> = emptyList(),
    val personalGoals: List<String> = emptyList(),
    val urgencyLevel: String = "LOW",
    val supportIntensity: String = "LIGHT",
    val communicationStyle: String? = null,
    val baselineAnxietyLevel: Int? = null,
    val baselineStressLevel: Int? = null,
    val baselineWellbeingLevel: Int? = null,
    val recommendedApproach: String? = null,
)

@Component
class UserServiceClient(
    @param:Value("\${app.user-service-url}") private val userServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(UserServiceClient::class.java)

    private val restClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(userServiceUrl)
            .defaultHeader("X-Internal-Key", internalKey)
            .build()
    }

    fun getPlanContext(userId: UUID): PlanContextResponse? =
        runCatching {
            restClient.get()
                .uri("/internal/users/$userId/plan-context")
                .retrieve()
                .body(PlanContextResponse::class.java)
        }.onFailure { logger.warn("Failed to fetch plan context for $userId: ${it.message}") }
         .getOrNull()
}
