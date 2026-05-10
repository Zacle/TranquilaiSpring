package com.tranquilai.activity.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.util.UUID

class DownstreamServiceClientsTest {

    private val restTemplate: RestTemplate = mock(RestTemplate::class.java)

    @Test
    fun `plan activity client posts complete by type with internal key`() {
        val userId = UUID.randomUUID()
        val client = PlanActivityClient(restTemplate, "http://plan-service", "secret")
        `when`(
            restTemplate.exchange(
                eq("http://plan-service/internal/plans/$userId/complete-by-type?type=MEDITATION"),
                eq(HttpMethod.POST),
                anyHttpEntity(),
                eq(Void::class.java),
            ),
        ).thenReturn(ResponseEntity(HttpStatus.OK))

        client.completeByType(userId, "MEDITATION")

        val entity = captureEntity()
        verify(restTemplate).exchange(
            eq("http://plan-service/internal/plans/$userId/complete-by-type?type=MEDITATION"),
            eq(HttpMethod.POST),
            entity.capture(),
            eq(Void::class.java),
        )
        assertEquals("secret", entity.value.headers.getFirst("X-Internal-Key"))
    }

    @Test
    fun `progress service client patches stats with internal key`() {
        val userId = UUID.randomUUID()
        val request = UpdateStatsClientRequest(sessionsIncrement = 1, minutesIncrement = 5, markDayActive = true)
        val client = ProgressServiceClient(restTemplate, "http://progress-service", "secret")
        `when`(
            restTemplate.exchange(
                eq("http://progress-service/internal/progress/stats/$userId"),
                eq(HttpMethod.PATCH),
                anyHttpEntity(),
                eq(Void::class.java),
            ),
        ).thenReturn(ResponseEntity(HttpStatus.OK))

        client.updateStats(userId, request)

        val entity = captureEntity()
        verify(restTemplate).exchange(
            eq("http://progress-service/internal/progress/stats/$userId"),
            eq(HttpMethod.PATCH),
            entity.capture(),
            eq(Void::class.java),
        )
        assertEquals("secret", entity.value.headers.getFirst("X-Internal-Key"))
        assertEquals(request, entity.value.body)
    }

    @Test
    fun `downstream clients rethrow failures so RabbitMQ listeners can retry`() {
        val userId = UUID.randomUUID()
        `when`(
            restTemplate.exchange(
                anyString(),
                anyHttpMethod(),
                anyHttpEntity(),
                eq(Void::class.java),
            ),
        ).thenThrow(RuntimeException("downstream unavailable"))

        assertThrows(RuntimeException::class.java) {
            PlanActivityClient(restTemplate, "http://plan-service", "secret").completeByType(userId, "JOURNALING")
        }
        assertThrows(RuntimeException::class.java) {
            ProgressServiceClient(restTemplate, "http://progress-service", "secret")
                .updateStats(userId, UpdateStatsClientRequest(markDayActive = true))
        }
    }

    private fun anyString(): String {
        any(String::class.java)
        return uninitialized()
    }

    private fun anyHttpMethod(): HttpMethod {
        any(HttpMethod::class.java)
        return uninitialized()
    }

    private fun anyHttpEntity(): HttpEntity<*> {
        any(HttpEntity::class.java)
        return uninitialized()
    }

    private fun captureEntity(): ArgumentCaptor<HttpEntity<*>> =
        ArgumentCaptor.forClass(HttpEntity::class.java) as ArgumentCaptor<HttpEntity<*>>

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
