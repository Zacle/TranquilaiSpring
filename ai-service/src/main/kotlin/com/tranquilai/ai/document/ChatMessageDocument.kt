package com.tranquilai.ai.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "chat_messages")
data class ChatMessageDocument(
    @Id val id: String,
    @Indexed val conversationId: String,
    @Indexed val userId: String,
    val content: String,
    /** USER | ASSISTANT | SYSTEM */
    val role: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    // Metadata (populated for ASSISTANT messages after analysis)
    val sentiment: String? = null,
    val detectedEmotions: List<String> = emptyList(),
    val crisisIndicators: List<String> = emptyList(),
)
