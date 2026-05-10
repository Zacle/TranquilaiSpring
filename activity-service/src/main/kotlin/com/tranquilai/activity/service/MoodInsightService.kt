package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.LogMoodRequest
import com.tranquilai.activity.messaging.ActivityMessaging
import com.tranquilai.activity.messaging.MoodInsightRequestedEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MoodInsightService(
    private val rabbitTemplate: RabbitTemplate,
) {

    fun generateAndSave(entryId: UUID, request: LogMoodRequest) {
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
}
