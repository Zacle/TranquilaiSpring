package com.tranquilai.ai.dto.request

import jakarta.validation.constraints.NotBlank

data class CreateConversationRequest(
    val languageCode: String = "en",
)

data class SendMessageRequest(
    @field:NotBlank
    val content: String,
    val languageCode: String = "en",
    /** AI greeting shown to the user before they typed — prepended to history for context continuity */
    val priorGreeting: String? = null,
    /** Client-generated UUID for the user message — kept to prevent duplicate IDs on poll */
    val messageId: String? = null,
    /** Client-generated UUID for the greeting message — kept to prevent duplicate IDs on poll */
    val greetingMessageId: String? = null,
    /** Client-generated UUID for the AI response — ensures mobile and server use the same ID */
    val aiResponseId: String? = null,
)

data class EndConversationRequest(
    /** When true, generate title/summary/topics/mood analysis before closing */
    val analyze: Boolean = true,
)
