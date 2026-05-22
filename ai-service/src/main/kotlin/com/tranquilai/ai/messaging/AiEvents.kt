package com.tranquilai.ai.messaging

import java.util.UUID

data class ChatStartedEvent(val userId: UUID)

data class ChatMessageRequestedEvent(
    val userId: String,
    val conversationId: String,
    val userMessageId: String,
    val content: String,
    val languageCode: String,
)
