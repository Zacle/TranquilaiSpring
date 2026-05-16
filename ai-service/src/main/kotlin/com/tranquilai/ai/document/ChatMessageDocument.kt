package com.tranquilai.ai.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "chat_messages")
@CompoundIndex(name = "idx_chat_messages_conversation_timestamp", def = "{'conversationId': 1, 'timestamp': 1}")
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
