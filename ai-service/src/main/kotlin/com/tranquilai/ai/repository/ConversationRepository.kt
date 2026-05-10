package com.tranquilai.ai.repository

import com.tranquilai.ai.document.ConversationDocument
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface ConversationRepository : MongoRepository<ConversationDocument, String> {
    fun findByUserIdOrderByLastMessageAtDesc(userId: String, pageable: Pageable): List<ConversationDocument>
    fun findByUserIdAndStatus(userId: String, status: String): List<ConversationDocument>
}
