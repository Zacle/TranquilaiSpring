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

class AiServiceClientTest {

    private val restTemplate: RestTemplate = mock(RestTemplate::class.java)
    private val client = AiServiceClient(restTemplate, "http://ai-service", "secret")

    @Test
    fun `generateMoodInsight posts internal request and returns insight`() {
        val request = MoodInsightClientRequest(
            moodScore = 6,
            emotions = listOf("calm"),
            notes = "Good",
            stressCauses = emptyList(),
            languageCode = "en",
        )
        `when`(
            restTemplate.exchange(
                eq("http://ai-service/internal/insights/mood"),
                eq(HttpMethod.POST),
                anyHttpEntity(),
                eq(MoodInsightClientResponse::class.java),
            ),
        ).thenReturn(ResponseEntity(MoodInsightClientResponse("Keep going."), HttpStatus.OK))

        val response = client.generateMoodInsight(request)

        assertEquals("Keep going.", response)
        val entity = captureEntity()
        verify(restTemplate).exchange(
            eq("http://ai-service/internal/insights/mood"),
            eq(HttpMethod.POST),
            entity.capture(),
            eq(MoodInsightClientResponse::class.java),
        )
        assertEquals("secret", entity.value.headers.getFirst("X-Internal-Key"))
        assertEquals(request, entity.value.body)
    }

    @Test
    fun `generateMoodInsight rethrows downstream failures so RabbitMQ listener can retry`() {
        `when`(
            restTemplate.exchange(
                eq("http://ai-service/internal/insights/mood"),
                eq(HttpMethod.POST),
                anyHttpEntity(),
                eq(MoodInsightClientResponse::class.java),
            ),
        ).thenThrow(RuntimeException("downstream unavailable"))

        assertThrows(RuntimeException::class.java) {
            client.generateMoodInsight(MoodInsightClientRequest(5, emptyList(), null, emptyList(), "en"))
        }
    }

    @Test
    fun `summarizeJournal posts internal request and returns summary`() {
        val request = JournalSummaryClientRequest("Prompt", "Content", "reflection", "en")
        val summary = JournalSummaryClientResponse("Summary", listOf("theme"), "calm", null)
        `when`(
            restTemplate.exchange(
                eq("http://ai-service/internal/insights/journal"),
                eq(HttpMethod.POST),
                anyHttpEntity(),
                eq(JournalSummaryClientResponse::class.java),
            ),
        ).thenReturn(ResponseEntity(summary, HttpStatus.OK))

        assertEquals(summary, client.summarizeJournal(request))
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
