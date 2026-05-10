package com.tranquilai.plan.service

import com.tranquilai.plan.client.PlanContextResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.ai.chat.client.ChatClient
import java.util.UUID

class PlanGeneratorServiceTest {

    private val chatClient: ChatClient = mock(ChatClient::class.java, RETURNS_DEEP_STUBS)
    private val service = PlanGeneratorService(chatClient)

    @Test
    fun `generate parses raw json returned by chat client`() {
        `when`(chatClient.prompt().user(anyString()).call().content()).thenReturn(
            """
            {
              "greeting": "Hola Alex",
              "motivationalMessage": "Un paso amable.",
              "activities": [
                {
                  "type": "MOOD_TRACKING",
                  "title": "Estado de animo",
                  "description": "Observa como estas.",
                  "prompt": null,
                  "durationMinutes": 3
                },
                {
                  "type": "CHAT_WITH_AI",
                  "title": "Conversar",
                  "description": "Habla con tu acompanante.",
                  "prompt": null,
                  "durationMinutes": 5
                }
              ]
            }
            """.trimIndent(),
        )

        val response = service.generate(context(), "es")

        assertEquals("Hola Alex", response.greeting)
        assertEquals(listOf("MOOD_TRACKING", "CHAT_WITH_AI"), response.activities.map { it.type })
    }

    @Test
    fun `generate extracts json from markdown style response`() {
        `when`(chatClient.prompt().user(anyString()).call().content()).thenReturn(
            """
            Here is the plan:
            {"greeting":"Hi","motivationalMessage":"Start gently.","activities":[{"type":"MOOD_TRACKING","title":"Mood","description":"Check in.","prompt":null,"durationMinutes":3}]}
            Thanks
            """.trimIndent(),
        )

        val response = service.generate(context(), "en")

        assertEquals("Hi", response.greeting)
        assertEquals("Mood", response.activities.single().title)
    }

    @Test
    fun `generate returns fallback plan when chat client fails`() {
        `when`(chatClient.prompt().user(anyString()).call().content()).thenThrow(RuntimeException("AI down"))

        val response = service.generate(context(firstName = "Maya"), "en")

        assertEquals("Good day, Maya!", response.greeting)
        assertTrue(response.activities.map { it.type }.containsAll(listOf("MOOD_TRACKING", "CHAT_WITH_AI")))
    }

    private fun context(firstName: String = "Alex") = PlanContextResponse(
        userId = UUID.randomUUID(),
        firstName = firstName,
        currentFeelingLevel = "ANXIOUS",
        stressCauses = listOf("work"),
        currentConcerns = listOf("sleep"),
        mentalProcessPreferences = listOf("breathing"),
        personalGoals = listOf("calm"),
        urgencyLevel = "MEDIUM",
        supportIntensity = "GUIDED",
        communicationStyle = "direct",
        baselineAnxietyLevel = 7,
        baselineStressLevel = 6,
        baselineWellbeingLevel = 4,
        recommendedApproach = "grounding",
    )
}
