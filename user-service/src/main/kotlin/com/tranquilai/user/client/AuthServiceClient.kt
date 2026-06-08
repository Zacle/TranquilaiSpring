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

@Component
class AuthServiceClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.auth-service-url}") private val authServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(AuthServiceClient::class.java)

    private fun headers() = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("X-Internal-Key", internalKey)
    }

    fun deactivateUser(userId: UUID) {
        try {
            restTemplate.exchange(
                "$authServiceUrl/internal/users/$userId/deactivate",
                HttpMethod.PUT,
                HttpEntity<Void>(headers()),
                Void::class.java,
            )
        } catch (ex: Exception) {
            logger.warn("Failed to deactivate user $userId in auth-service: ${ex.message}")
        }
    }

    fun deleteUser(userId: UUID) {
        try {
            deleteUserOrThrow(userId)
        } catch (ex: Exception) {
            logger.warn("Failed to delete user $userId from auth-service: ${ex.message}")
        }
    }

    fun deleteUserOrThrow(userId: UUID) {
        restTemplate.exchange(
            "$authServiceUrl/internal/users/$userId",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers()),
            Void::class.java,
        )
    }
}
