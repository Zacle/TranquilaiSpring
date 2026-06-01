package com.tranquilai.activity.service

import com.tranquilai.activity.client.SubscriptionServiceClient
import com.tranquilai.activity.dto.request.CreateJournalEntryRequest
import com.tranquilai.activity.messaging.ActivityMessaging
import com.tranquilai.activity.messaging.JournalSummaryRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JournalSummaryService(
    private val rabbitTemplate: RabbitTemplate,
    private val subscriptionServiceClient: SubscriptionServiceClient,
) {
    private val logger = LoggerFactory.getLogger(JournalSummaryService::class.java)

    fun summarizeAndSave(userId: UUID, entryId: UUID, request: CreateJournalEntryRequest) {
        val entitlement = subscriptionServiceClient.checkEntitlement(userId, FEATURE_JOURNAL_SUMMARY)
        if (!entitlement.allowed) {
            logger.info("Skipping journal summary generation for userId=$userId plan=${entitlement.plan}")
            return
        }

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

    private companion object {
        const val FEATURE_JOURNAL_SUMMARY = "JOURNAL_SUMMARY"
    }
}
