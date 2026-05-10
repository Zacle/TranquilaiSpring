package com.tranquilai.ai.service

import com.tranquilai.ai.messaging.AiMessaging
import com.tranquilai.ai.messaging.ChatStartedEvent
import org.junit.jupiter.api.Test
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.UUID

class ActivityCompletionServiceTest {

    private val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
    private val service = ActivityCompletionService(rabbitTemplate)

    @Test
    fun `onChatStarted publishes durable chat started event`() {
        val userId = UUID.randomUUID()

        service.onChatStarted(userId.toString())

        verify(rabbitTemplate, times(1)).convertAndSend(
            eq(AiMessaging.Exchange),
            eq(AiMessaging.ChatPlanRoutingKey),
            eq(ChatStartedEvent(userId)),
        )
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq(AiMessaging.Exchange),
            eq(AiMessaging.ChatProgressRoutingKey),
            eq(ChatStartedEvent(userId)),
        )
    }
}
