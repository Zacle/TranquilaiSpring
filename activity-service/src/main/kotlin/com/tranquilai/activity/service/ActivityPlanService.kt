package com.tranquilai.activity.service

import com.tranquilai.activity.messaging.ActivityMessaging
import com.tranquilai.activity.messaging.PlanActivityCompletedEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID

/** Publishes durable plan-completion side effects after activity writes. */
@Service
class ActivityPlanService(private val rabbitTemplate: RabbitTemplate) {

    fun onMoodLogged(userId: UUID) = publish(userId, "MOOD_TRACKING")

    fun onJournalCreated(userId: UUID) = publish(userId, "JOURNALING")

    fun onBreathingLogged(userId: UUID) = publish(userId, "BREATHING_EXERCISE")

    fun onMeditationLogged(userId: UUID) = publish(userId, "MEDITATION")

    fun onAffirmationViewed(userId: UUID) = publish(userId, "AFFIRMATION")

    private fun publish(userId: UUID, activityType: String) =
        rabbitTemplate.convertAndSend(
            ActivityMessaging.Exchange,
            ActivityMessaging.PlanRoutingKey,
            PlanActivityCompletedEvent(userId, activityType),
        )
}
