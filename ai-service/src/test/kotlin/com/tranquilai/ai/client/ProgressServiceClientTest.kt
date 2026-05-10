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

class ProgressServiceClientTest {

    private val restTemplate: RestTemplate = mock(RestTemplate::class.java)
    private val client = ProgressServiceClient(restTemplate, "http://progress-service:8087", "internal-key")

    @Test
    fun `updateStats sends patch request with internal key and body`() {
        val request = UpdateStatsRequest(chatSessionsIncrement = 1, markDayActive = true)

        client.updateStats("user-123", request)

        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).exchange(
            eq("http://progress-service:8087/internal/progress/stats/user-123"),
            eq(HttpMethod.PATCH),
            entityCaptor.capture(),
            eq(Void::class.java),
        )
        assertEquals("internal-key", entityCaptor.value.headers.getFirst("X-Internal-Key"))
        assertEquals(request, entityCaptor.value.body)
    }

    @Test
    fun `updateStats rethrows downstream failures so RabbitMQ listener can retry`() {
        `when`(
            restTemplate.exchange(
                eq("http://progress-service:8087/internal/progress/stats/user-123"),
                eq(HttpMethod.PATCH),
                anyEntity(),
                eq(Void::class.java),
            ),
        ).thenThrow(RuntimeException("down"))

        assertThrows(RuntimeException::class.java) {
            client.updateStats("user-123", UpdateStatsRequest(chatSessionsIncrement = 1))
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
