package com.tranquilai.activity.service

import com.tranquilai.activity.messaging.ActivityMessaging
import com.tranquilai.activity.messaging.PlanActivityCompletedEvent
import com.tranquilai.activity.messaging.ProgressStatsEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.UUID

class ActivitySideEffectServicesTest {

    @Test
    fun `progress service publishes each activity as progress event`() {
        val userId = UUID.randomUUID()
        val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
        val service = ActivityProgressService(rabbitTemplate)

        service.onMoodLogged(userId)
        service.onJournalCreated(userId)
        service.onBreathingLogged(userId, 125)
        service.onMeditationLogged(userId, 360)
        service.onAffirmationViewed(userId)

        val eventCaptor = ArgumentCaptor.forClass(ProgressStatsEvent::class.java)
        verify(rabbitTemplate, org.mockito.Mockito.times(5)).convertAndSend(
            org.mockito.Mockito.eq(ActivityMessaging.Exchange),
            org.mockito.Mockito.eq(ActivityMessaging.ProgressRoutingKey),
            eventCaptor.capture(),
        )
        assertEquals(
            listOf(
                ProgressStatsEvent(userId = userId, moodEntriesIncrement = 1, markDayActive = true),
                ProgressStatsEvent(userId = userId, journalEntriesIncrement = 1, markDayActive = true),
                ProgressStatsEvent(userId = userId, sessionsIncrement = 1, minutesIncrement = 2, markDayActive = true),
                ProgressStatsEvent(userId = userId, sessionsIncrement = 1, minutesIncrement = 6, markDayActive = true),
                ProgressStatsEvent(userId = userId, markDayActive = true),
            ),
            eventCaptor.allValues,
        )
    }

    @Test
    fun `plan service publishes each activity as plan event`() {
        val userId = UUID.randomUUID()
        val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
        val service = ActivityPlanService(rabbitTemplate)

        service.onMoodLogged(userId)
        service.onJournalCreated(userId)
        service.onBreathingLogged(userId)
        service.onMeditationLogged(userId)
        service.onAffirmationViewed(userId)

        val eventCaptor = ArgumentCaptor.forClass(PlanActivityCompletedEvent::class.java)
        verify(rabbitTemplate, org.mockito.Mockito.times(5)).convertAndSend(
            org.mockito.Mockito.eq(ActivityMessaging.Exchange),
            org.mockito.Mockito.eq(ActivityMessaging.PlanRoutingKey),
            eventCaptor.capture(),
        )
        assertEquals(
            listOf("MOOD_TRACKING", "JOURNALING", "BREATHING_EXERCISE", "MEDITATION", "AFFIRMATION"),
            eventCaptor.allValues.map { it.activityType },
        )
    }
}
