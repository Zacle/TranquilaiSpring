package com.tranquilai.ai.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "conversations")
data class ConversationDocument(
    @Id val id: String,
    @Indexed val userId: String,
    val title: String? = null,
    val summary: String? = null,
    val keyTopics: List<String> = emptyList(),
    val moodAtStart: String? = null,
    val moodAtEnd: String? = null,
    /** ACTIVE | COMPLETED | ARCHIVED */
    val status: String = "ACTIVE",
    val startedAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val messageCount: Int = 0,
    val languageCode: String = "en",
)
