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
class AiServiceClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.ai-service-url}") private val aiServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private fun headers() = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("X-Internal-Key", internalKey)
    }

    fun deleteChatHistory(userId: UUID) {
        restTemplate.exchange(
            "$aiServiceUrl/internal/users/$userId/chat-history",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers()),
            Void::class.java,
        )
    }
}
