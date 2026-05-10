package com.tranquilai.user.client

import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.util.UUID

class AuthServiceClientTest {

    private val restTemplate: RestTemplate = mock(RestTemplate::class.java)
    private val client = AuthServiceClient(restTemplate, "http://auth-service:8081", "internal-key")

    @Test
    fun `deactivateUser calls auth internal endpoint with internal key`() {
        val userId = UUID.randomUUID()

        client.deactivateUser(userId)

        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).exchange(
            eq("http://auth-service:8081/internal/users/$userId/deactivate"),
            eq(HttpMethod.PUT),
            entityCaptor.capture(),
            eq(Void::class.java),
        )
        assert(entityCaptor.value.headers.getFirst("X-Internal-Key") == "internal-key")
    }

    @Test
    fun `deleteUser calls auth internal endpoint with internal key`() {
        val userId = UUID.randomUUID()

        client.deleteUser(userId)

        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).exchange(
            eq("http://auth-service:8081/internal/users/$userId"),
            eq(HttpMethod.DELETE),
            entityCaptor.capture(),
            eq(Void::class.java),
        )
        assert(entityCaptor.value.headers.getFirst("X-Internal-Key") == "internal-key")
    }

    @Test
    fun `deactivateUser swallows downstream failures`() {
        val userId = UUID.randomUUID()
        `when`(
            restTemplate.exchange(
                eq("http://auth-service:8081/internal/users/$userId/deactivate"),
                eq(HttpMethod.PUT),
                org.mockito.Mockito.any(HttpEntity::class.java),
                eq(Void::class.java),
            ),
        ).thenThrow(RuntimeException("down"))

        client.deactivateUser(userId)
    }
}
