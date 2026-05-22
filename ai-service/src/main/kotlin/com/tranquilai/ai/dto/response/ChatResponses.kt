package com.tranquilai.ai.dto.response

data class MessageResponse(
    val id: String,
    val conversationId: String,
    val content: String,
    val role: String,
    val timestamp: Long,
    val sentiment: String? = null,
    val detectedEmotions: List<String> = emptyList(),
)

data class ConversationResponse(
    val id: String,
    val userId: String,
    val title: String?,
    val summary: String?,
    val keyTopics: List<String>,
    val moodAtStart: String?,
    val moodAtEnd: String?,
    val status: String,
    val messageCount: Int,
    val languageCode: String,
    val startedAt: Long,
    val lastMessageAt: Long,
    val endedAt: Long?,
)

data class ConversationWithMessagesResponse(
    val conversation: ConversationResponse,
    val messages: List<MessageResponse>,
)

data class SendMessageResponse(
    val userMessage: MessageResponse,
    val aiResponse: MessageResponse? = null,
    val conversationId: String,
    val status: String = "COMPLETED",
)
