package com.tranquilai.ai.service

import com.tranquilai.ai.dto.request.GenerateAffirmationRequest
import com.tranquilai.ai.dto.request.GenerateGreetingRequest
import com.tranquilai.ai.dto.request.MoodInsightRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.ai.chat.client.ChatClient

class InsightServiceTest {

    private val chatClient: ChatClient = mock(ChatClient::class.java, RETURNS_DEEP_STUBS)
    private val service = InsightService(chatClient)

    @Test
    fun `generateGreeting returns trimmed ai content`() {
        `when`(chatClient.prompt().user(anyString()).call().content()).thenReturn("  Hello, Test  ")

        val response = service.generateGreeting(GenerateGreetingRequest(firstName = "Test"))

        assertEquals("Hello, Test", response.greeting)
    }

    @Test
    fun `generateMoodInsight falls back by mood score when ai fails`() {
        `when`(chatClient.prompt().user(anyString()).call().content()).thenThrow(RuntimeException("down"))

        val response = service.generateMoodInsight(MoodInsightRequest(moodScore = 2))

        assertTrue(response.insight.contains("today has been tough"))
    }

    @Test
    fun `generateAffirmation falls back when ai fails`() {
        `when`(chatClient.prompt().user(anyString()).call().content()).thenThrow(RuntimeException("down"))

        val response = service.generateAffirmation(GenerateAffirmationRequest())

        assertEquals("I am worthy of peace and joy.", response.affirmation)
    }
}
