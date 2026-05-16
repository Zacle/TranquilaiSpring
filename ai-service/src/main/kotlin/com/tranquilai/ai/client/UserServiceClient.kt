package com.tranquilai.ai.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class UserServiceClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.user-service-url}") private val userServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(UserServiceClient::class.java)

    fun getFirstName(userId: String): String? =
        try {
            restTemplate.exchange(
                "$userServiceUrl/internal/users/$userId",
                HttpMethod.GET,
                HttpEntity(null, internalHeaders()),
                UserResponse::class.java,
            ).body?.firstName?.takeIf { it.isNotBlank() }
        } catch (ex: Exception) {
            logger.warn("Failed to fetch user profile for conversation analysis userId=$userId: ${ex.message}")
            null
        }

    private fun internalHeaders() = HttpHeaders().apply {
        set("X-Internal-Key", internalKey)
        contentType = MediaType.APPLICATION_JSON
    }
}

data class UserResponse(
    val id: String? = null,
    val email: String? = null,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
)
