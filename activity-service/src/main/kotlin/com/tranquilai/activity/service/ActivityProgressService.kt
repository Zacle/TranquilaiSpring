package com.tranquilai.activity.service

import com.tranquilai.activity.messaging.ActivityMessaging
import com.tranquilai.activity.messaging.ProgressStatsEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID

/** Publishes durable progress side effects so downstream outages do not block activity writes. */
@Service
class ActivityProgressService(private val rabbitTemplate: RabbitTemplate) {

    fun onMoodLogged(userId: UUID) {
        publish(
            ProgressStatsEvent(userId = userId, moodEntriesIncrement = 1, markDayActive = true),
        )
    }

    fun onJournalCreated(userId: UUID) {
        publish(
            ProgressStatsEvent(userId = userId, journalEntriesIncrement = 1, markDayActive = true),
        )
    }

    fun onBreathingLogged(userId: UUID, actualDurationSeconds: Int) {
        publish(
            ProgressStatsEvent(
                userId = userId,
                sessionsIncrement = 1,
                minutesIncrement = actualDurationSeconds / 60,
                markDayActive = true,
            ),
        )
    }

    fun onMeditationLogged(userId: UUID, actualDurationSeconds: Int) {
        publish(
            ProgressStatsEvent(
                userId = userId,
                sessionsIncrement = 1,
                minutesIncrement = actualDurationSeconds / 60,
                markDayActive = true,
            ),
        )
    }

    fun onAffirmationViewed(userId: UUID) {
        publish(ProgressStatsEvent(userId = userId, markDayActive = true))
    }

    private fun publish(event: ProgressStatsEvent) =
        rabbitTemplate.convertAndSend(ActivityMessaging.Exchange, ActivityMessaging.ProgressRoutingKey, event)
}
