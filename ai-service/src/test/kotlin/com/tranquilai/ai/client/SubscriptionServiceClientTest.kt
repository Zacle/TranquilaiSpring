package com.tranquilai.ai.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class SubscriptionServiceClientTest {

    private val restTemplate: RestTemplate = mock(RestTemplate::class.java)
    private val client = SubscriptionServiceClient(restTemplate, "http://subscription-service:8089", "internal-key")

    @Test
    fun `checkUsage calls internal usage endpoint with internal key`() {
        `when`(
            restTemplate.exchange(
                eq("http://subscription-service:8089/internal/subscriptions/usage?userId=user-123&feature=AI_CHAT"),
                eq(HttpMethod.GET),
                anyEntity(),
                eq(UsageResponse::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(UsageResponse(allowed = false, used = 3, limit = 3, remaining = 0, plan = "FREE")))

        val response = client.checkUsage("user-123", "AI_CHAT")

        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).exchange(
            eq("http://subscription-service:8089/internal/subscriptions/usage?userId=user-123&feature=AI_CHAT"),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(UsageResponse::class.java),
        )
        assertEquals("internal-key", entityCaptor.value.headers.getFirst("X-Internal-Key"))
        assertEquals(false, response.allowed)
        assertEquals(3, response.used)
    }

    @Test
    fun `checkEntitlement falls back open when subscription service fails`() {
        `when`(
            restTemplate.exchange(
                eq("http://subscription-service:8089/internal/subscriptions/entitlement?userId=user-123&feature=JOURNAL_SUMMARY"),
                eq(HttpMethod.GET),
                anyEntity(),
                eq(EntitlementResponse::class.java),
            ),
        ).thenThrow(RuntimeException("down"))

        val response = client.checkEntitlement("user-123", "JOURNAL_SUMMARY")

        assertTrue(response.allowed)
        assertEquals("UNKNOWN", response.plan)
    }

    @Test
    fun `incrementUsage posts feature body with internal key`() {
        client.incrementUsage("user-123", "AI_CHAT")

        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).exchange(
            eq("http://subscription-service:8089/internal/subscriptions/usage/increment?userId=user-123"),
            eq(HttpMethod.POST),
            entityCaptor.capture(),
            eq(Void::class.java),
        )
        val body = entityCaptor.value.body as IncrementUsageRequest
        assertEquals("internal-key", entityCaptor.value.headers.getFirst("X-Internal-Key"))
        assertEquals("AI_CHAT", body.feature)
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyEntity(): HttpEntity<Any> {
        org.mockito.Mockito.any(HttpEntity::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
