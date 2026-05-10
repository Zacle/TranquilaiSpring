package com.tranquilai.activity.messaging

import com.tranquilai.activity.client.AiServiceClient
import com.tranquilai.activity.client.PlanActivityClient
import com.tranquilai.activity.client.ProgressServiceClient
import com.tranquilai.activity.client.UpdateStatsClientRequest
import com.tranquilai.activity.repository.JournalEntryRepository
import com.tranquilai.activity.repository.MoodEntryRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.UUID

class ActivityEventListenersTest {

    private val progressClient: ProgressServiceClient = mock(ProgressServiceClient::class.java)
    private val planClient: PlanActivityClient = mock(PlanActivityClient::class.java)
    private val listener = ActivityEventListeners(
        progressClient = progressClient,
        planClient = planClient,
        aiClient = mock(AiServiceClient::class.java),
        moodRepo = mock(MoodEntryRepository::class.java),
        journalRepo = mock(JournalEntryRepository::class.java),
        rabbitTemplate = mock(RabbitTemplate::class.java),
    )

    @Test
    fun `progress event delegates to progress service client`() {
        val userId = UUID.randomUUID()

        listener.handleProgressEvent(
            ProgressStatsEvent(
                userId = userId,
                sessionsIncrement = 1,
                minutesIncrement = 4,
                markDayActive = true,
            ),
        )

        verify(progressClient).updateStats(
            userId,
            UpdateStatsClientRequest(sessionsIncrement = 1, minutesIncrement = 4, markDayActive = true),
        )
    }

    @Test
    fun `plan event delegates to plan service client`() {
        val userId = UUID.randomUUID()

        listener.handlePlanEvent(PlanActivityCompletedEvent(userId, "MEDITATION"))

        verify(planClient).completeByType(userId, "MEDITATION")
    }
}
