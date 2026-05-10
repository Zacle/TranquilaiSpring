package com.tranquilai.ai.messaging

import com.tranquilai.ai.client.PlanActivityClient
import com.tranquilai.ai.client.ProgressServiceClient
import com.tranquilai.ai.client.UpdateStatsRequest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.UUID

class AiEventListenersTest {

    private val planClient: PlanActivityClient = mock(PlanActivityClient::class.java)
    private val progressClient: ProgressServiceClient = mock(ProgressServiceClient::class.java)
    private val listener = AiEventListeners(planClient, progressClient)

    @Test
    fun `chat plan event completes AI plan activity`() {
        val userId = UUID.randomUUID()

        listener.handleChatPlan(ChatStartedEvent(userId))

        verify(planClient).completeByType(userId.toString(), "CHAT_WITH_AI")
    }

    @Test
    fun `chat progress event updates chat progress`() {
        val userId = UUID.randomUUID()

        listener.handleChatProgress(ChatStartedEvent(userId))

        verify(progressClient).updateStats(
            userId.toString(),
            UpdateStatsRequest(chatSessionsIncrement = 1, markDayActive = true),
        )
    }
}
