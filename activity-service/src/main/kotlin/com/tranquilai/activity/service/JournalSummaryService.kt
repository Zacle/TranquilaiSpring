package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.CreateJournalEntryRequest
import com.tranquilai.activity.messaging.ActivityMessaging
import com.tranquilai.activity.messaging.JournalSummaryRequestedEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JournalSummaryService(
    private val rabbitTemplate: RabbitTemplate,
) {

    fun summarizeAndSave(entryId: UUID, request: CreateJournalEntryRequest) {
        rabbitTemplate.convertAndSend(
            ActivityMessaging.Exchange,
            ActivityMessaging.AiJournalSummaryRoutingKey,
            JournalSummaryRequestedEvent(
                entryId = entryId,
                promptText = request.promptText ?: "",
                content = request.content,
                category = request.category,
                languageCode = request.languageCode,
            ),
        )
    }
}
