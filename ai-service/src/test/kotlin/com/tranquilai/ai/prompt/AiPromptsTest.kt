package com.tranquilai.ai.prompt

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiPromptsTest {

    @Test
    fun `chat system prompt blocks detached ai identity replies`() {
        val prompt = AiPrompts.therapistSystemPrompt()

        assertTrue(prompt.contains("Never say \"as an AI\""))
        assertTrue(prompt.contains("I don't have feelings"))
        assertTrue(prompt.contains("answer naturally, warmly, and briefly"))
    }

    @Test
    fun `conversation summary prompt does not describe companion as ai`() {
        val prompt = AiPrompts.conversationSummaryPrompt(
            messagesText = "USER: How are you?\nASSISTANT: I'm glad you're here.",
            firstName = "Maya",
        )

        assertTrue(prompt.contains("between Maya and their wellness companion"))
        assertFalse(prompt.contains("AI wellness companion"))
    }
}
