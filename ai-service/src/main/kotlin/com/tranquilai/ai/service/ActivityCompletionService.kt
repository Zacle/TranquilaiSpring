package com.tranquilai.ai.service

import com.tranquilai.ai.messaging.AiMessaging
import com.tranquilai.ai.messaging.ChatStartedEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID

/** Publishes durable chat-started side effects so downstream outages do not block chat. */
@Service
class ActivityCompletionService(
    private val rabbitTemplate: RabbitTemplate,
) {
    fun onChatStarted(userId: String) {
        val event = ChatStartedEvent(UUID.fromString(userId))
        rabbitTemplate.convertAndSend(AiMessaging.Exchange, AiMessaging.ChatPlanRoutingKey, event)
        rabbitTemplate.convertAndSend(AiMessaging.Exchange, AiMessaging.ChatProgressRoutingKey, event)
    }
}
