package com.tranquilai.activity.service

import com.tranquilai.activity.client.SubscriptionServiceClient
import com.tranquilai.activity.dto.request.LogMoodRequest
import com.tranquilai.activity.messaging.ActivityMessaging
import com.tranquilai.activity.messaging.MoodInsightRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MoodInsightService(
    private val rabbitTemplate: RabbitTemplate,
    private val subscriptionServiceClient: SubscriptionServiceClient,
) {
    private val logger = LoggerFactory.getLogger(MoodInsightService::class.java)

    fun generateAndSave(userId: UUID, entryId: UUID, request: LogMoodRequest) {
        val entitlement = subscriptionServiceClient.checkEntitlement(userId, FEATURE_ADVANCED_MOOD_INSIGHTS)
        if (!entitlement.allowed) {
            logger.info("Skipping mood insight generation for userId=$userId plan=${entitlement.plan}")
            return
        }

        rabbitTemplate.convertAndSend(
            ActivityMessaging.Exchange,
            ActivityMessaging.AiMoodInsightRoutingKey,
            MoodInsightRequestedEvent(
                entryId = entryId,
                moodScore = request.moodScore,
                emotions = request.emotions,
                notes = request.notes,
                stressCauses = request.stressCauses,
                languageCode = request.languageCode,
            ),
        )
    }

    private companion object {
        const val FEATURE_ADVANCED_MOOD_INSIGHTS = "ADVANCED_MOOD_INSIGHTS"
    }
}
