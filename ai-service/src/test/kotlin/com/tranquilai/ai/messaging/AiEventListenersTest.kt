package com.tranquilai.ai.messaging

import com.tranquilai.ai.client.PlanActivityClient
import com.tranquilai.ai.client.ProgressServiceClient
import com.tranquilai.ai.client.UpdateStatsRequest
import com.tranquilai.ai.service.ChatService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.UUID

class AiEventListenersTest {

    private val planClient: PlanActivityClient = mock(PlanActivityClient::class.java)
    private val progressClient: ProgressServiceClient = mock(ProgressServiceClient::class.java)
    private val chatService: ChatService = mock(ChatService::class.java)
    private val listener = AiEventListeners(planClient, progressClient, chatService)

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

    @Test
    fun `chat message request triggers queued AI response generation`() {
        val event = ChatMessageRequestedEvent(
            userId = "user-123",
            conversationId = "conv-1",
            userMessageId = "msg-1",
            content = "hello",
            languageCode = "en",
        )

        listener.handleChatMessageRequested(event)

        verify(chatService).generateQueuedAiResponse(event)
    }
}
