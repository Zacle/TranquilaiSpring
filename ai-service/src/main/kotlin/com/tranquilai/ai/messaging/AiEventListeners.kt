package com.tranquilai.ai.messaging

import com.tranquilai.ai.client.PlanActivityClient
import com.tranquilai.ai.client.ProgressServiceClient
import com.tranquilai.ai.client.UpdateStatsRequest
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class AiEventListeners(
    private val planClient: PlanActivityClient,
    private val progressClient: ProgressServiceClient,
) {

    @RabbitListener(queues = [AiMessaging.ChatPlanQueue])
    fun handleChatPlan(event: ChatStartedEvent) {
        planClient.completeByType(event.userId.toString(), "CHAT_WITH_AI")
    }

    @RabbitListener(queues = [AiMessaging.ChatProgressQueue])
    fun handleChatProgress(event: ChatStartedEvent) {
        progressClient.updateStats(event.userId.toString(), UpdateStatsRequest(chatSessionsIncrement = 1, markDayActive = true))
    }
}
