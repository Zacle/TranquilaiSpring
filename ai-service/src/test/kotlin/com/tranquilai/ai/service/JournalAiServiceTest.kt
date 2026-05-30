package com.tranquilai.ai.service

import com.tranquilai.ai.client.EntitlementResponse
import com.tranquilai.ai.client.SubscriptionServiceClient
import com.tranquilai.ai.dto.request.SummarizeJournalRequest
import com.tranquilai.ai.exception.PaymentRequiredException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.ai.chat.client.ChatClient

class JournalAiServiceTest {

    private val chatClient: ChatClient = mock(ChatClient::class.java, RETURNS_DEEP_STUBS)
    private val subscriptionClient: SubscriptionServiceClient = mock(SubscriptionServiceClient::class.java)
    private val service = JournalAiService(chatClient, subscriptionClient, AiCallExecutor())

    @Test
    fun `summarize checks premium entitlement and parses json response`() {
        `when`(subscriptionClient.checkEntitlement("user-123", "JOURNAL_SUMMARY"))
            .thenReturn(EntitlementResponse(allowed = true, plan = "PREMIUM"))
        `when`(chatClient.prompt().user(anyString()).call().content()).thenReturn(
            """
            Here is the result:
            {"summary":"You processed work stress","keyThemes":["work","stress"],"emotionalTone":"reflective","suggestedFollowUp":"What helped today?"}
            """.trimIndent(),
        )

        val response = service.summarize("user-123", request())

        assertEquals("You processed work stress", response.summary)
        assertEquals(listOf("work", "stress"), response.keyThemes)
        assertEquals("reflective", response.emotionalTone)
        assertEquals("What helped today?", response.suggestedFollowUp)
        verify(subscriptionClient).checkEntitlement("user-123", "JOURNAL_SUMMARY")
    }

    @Test
    fun `summarize throws payment required when entitlement is denied`() {
        `when`(subscriptionClient.checkEntitlement("user-123", "JOURNAL_SUMMARY"))
            .thenReturn(EntitlementResponse(allowed = false, plan = "FREE"))

        assertThrows(PaymentRequiredException::class.java) {
            service.summarize("user-123", request())
        }
    }

    @Test
    fun `summarizeInternal skips entitlement and falls back on malformed ai response`() {
        `when`(chatClient.prompt().user(anyString()).call().content()).thenReturn("not-json")

        val response = service.summarizeInternal(request(content = "I felt anxious before work."))

        assertEquals(listOf("self-reflection"), response.keyThemes)
        assertEquals("processing", response.emotionalTone)
    }

    @Test
    fun `summarizeInternal uses localized fallback on malformed ai response`() {
        `when`(chatClient.prompt().user(anyString()).call().content()).thenReturn("not-json")

        val response = service.summarizeInternal(
            request(content = "Je me suis senti anxieux avant le travail.", languageCode = "fr"),
        )

        assertEquals(listOf("autoréflexion"), response.keyThemes)
        assertEquals("en cours d’intégration", response.emotionalTone)
    }

    private fun request(
        content: String = "I felt stressed about work today.",
        languageCode: String = "en",
    ) =
        SummarizeJournalRequest(
            promptText = "How did your day feel?",
            content = content,
            category = "reflection",
            stressCauses = listOf("work"),
            languageCode = languageCode,
        )
}
