package com.tranquilai.ai.repository

import com.tranquilai.ai.document.ChatMessageDocument
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface ChatMessageRepository : MongoRepository<ChatMessageDocument, String> {
    fun findByConversationIdOrderByTimestampAsc(conversationId: String): List<ChatMessageDocument>
    fun findByConversationIdOrderByTimestampDesc(conversationId: String, pageable: Pageable): List<ChatMessageDocument>
    fun countByConversationId(conversationId: String): Long
    fun deleteByConversationId(conversationId: String)
}
