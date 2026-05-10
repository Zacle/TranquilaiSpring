package com.tranquilai.ai.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

class PlanActivityClientTest {

    private val restTemplate: RestTemplate = mock(RestTemplate::class.java)
    private val client = PlanActivityClient(restTemplate, "http://plan-service:8086", "internal-key")

    @Test
    fun `completeByType sends post request with internal key`() {
        client.completeByType("user-123", "CHAT_WITH_AI")

        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).exchange(
            eq("http://plan-service:8086/internal/plans/user-123/complete-by-type?type=CHAT_WITH_AI"),
            eq(HttpMethod.POST),
            entityCaptor.capture(),
            eq(Void::class.java),
        )
        assertEquals("internal-key", entityCaptor.value.headers.getFirst("X-Internal-Key"))
    }

    @Test
    fun `completeByType rethrows downstream failures so RabbitMQ listener can retry`() {
        `when`(
            restTemplate.exchange(
                eq("http://plan-service:8086/internal/plans/user-123/complete-by-type?type=CHAT_WITH_AI"),
                eq(HttpMethod.POST),
                anyEntity(),
                eq(Void::class.java),
            ),
        ).thenThrow(RuntimeException("down"))

        assertThrows(RuntimeException::class.java) {
            client.completeByType("user-123", "CHAT_WITH_AI")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyEntity(): HttpEntity<Any> {
        org.mockito.Mockito.any(HttpEntity::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
