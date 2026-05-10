package com.tranquilai.auth.client

import com.tranquilai.auth.entity.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Client for making internal calls to user-service.
 * Uses X-Internal-Key header for service authentication.
 */
@Component
class UserServiceClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.user-service-url}") private val userServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(UserServiceClient::class.java)

    fun createUser(user: User) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Internal-Key", internalKey)
        }
        val body = mapOf(
            "id" to user.id.toString(),
            "email" to user.email,
            "username" to user.username,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
        )
        runCatching {
            restTemplate.postForObject("$userServiceUrl/internal/users", HttpEntity(body, headers), Any::class.java)
        }.onFailure { ex ->
            // Log but don't fail — user record in auth DB is the source of truth for auth
            logger.error("Failed to sync user ${user.id} to user-service: ${ex.message}", ex)
        }
    }
}
