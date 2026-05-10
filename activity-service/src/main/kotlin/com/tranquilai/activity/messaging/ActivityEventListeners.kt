package com.tranquilai.activity.messaging

import com.tranquilai.activity.client.AiServiceClient
import com.tranquilai.activity.client.JournalSummaryClientRequest
import com.tranquilai.activity.client.MoodInsightClientRequest
import com.tranquilai.activity.client.PlanActivityClient
import com.tranquilai.activity.client.ProgressServiceClient
import com.tranquilai.activity.client.UpdateStatsClientRequest
import com.tranquilai.activity.repository.JournalEntryRepository
import com.tranquilai.activity.repository.MoodEntryRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ActivityEventListeners(
    private val progressClient: ProgressServiceClient,
    private val planClient: PlanActivityClient,
    private val aiClient: AiServiceClient,
    private val moodRepo: MoodEntryRepository,
    private val journalRepo: JournalEntryRepository,
    private val rabbitTemplate: RabbitTemplate,
) {
    private val logger = LoggerFactory.getLogger(ActivityEventListeners::class.java)

    @RabbitListener(queues = [ActivityMessaging.ProgressQueue])
    fun handleProgressEvent(event: ProgressStatsEvent) {
        progressClient.updateStats(
            event.userId,
            UpdateStatsClientRequest(
                sessionsIncrement = event.sessionsIncrement,
                minutesIncrement = event.minutesIncrement,
                moodEntriesIncrement = event.moodEntriesIncrement,
                journalEntriesIncrement = event.journalEntriesIncrement,
                markDayActive = event.markDayActive,
            ),
        )
    }

    @RabbitListener(queues = [ActivityMessaging.PlanQueue])
    fun handlePlanEvent(event: PlanActivityCompletedEvent) {
        planClient.completeByType(event.userId, event.activityType)
    }

    @RabbitListener(queues = [ActivityMessaging.AiMoodInsightQueue])
    fun handleMoodInsightEvent(event: MoodInsightRequestedEvent) {
        val insight = aiClient.generateMoodInsight(
            MoodInsightClientRequest(
                moodScore = event.moodScore,
                emotions = event.emotions,
                notes = event.notes,
                stressCauses = event.stressCauses,
                languageCode = event.languageCode,
            ),
        ) ?: return

        rabbitTemplate.convertAndSend(
            ActivityMessaging.Exchange,
            ActivityMessaging.AiMoodInsightResultRoutingKey,
            MoodInsightGeneratedEvent(event.entryId, insight),
        )
    }

    @RabbitListener(queues = [ActivityMessaging.AiMoodInsightResultQueue])
    @Transactional
    fun handleMoodInsightResultEvent(event: MoodInsightGeneratedEvent) {
        moodRepo.findById(event.entryId).ifPresent { entry ->
            entry.aiInsight = event.insight
            moodRepo.save(entry)
            logger.debug("AI insight saved for mood entry {}", event.entryId)
        }
    }

    @RabbitListener(queues = [ActivityMessaging.AiJournalSummaryQueue])
    fun handleJournalSummaryEvent(event: JournalSummaryRequestedEvent) {
        val summary = aiClient.summarizeJournal(
            JournalSummaryClientRequest(
                promptText = event.promptText,
                content = event.content,
                category = event.category,
                languageCode = event.languageCode,
            ),
        ) ?: return

        rabbitTemplate.convertAndSend(
            ActivityMessaging.Exchange,
            ActivityMessaging.AiJournalSummaryResultRoutingKey,
            JournalSummaryGeneratedEvent(
                entryId = event.entryId,
                summary = summary.summary,
                keyThemes = summary.keyThemes,
                emotionalTone = summary.emotionalTone,
            ),
        )
    }

    @RabbitListener(queues = [ActivityMessaging.AiJournalSummaryResultQueue])
    @Transactional
    fun handleJournalSummaryResultEvent(event: JournalSummaryGeneratedEvent) {
        journalRepo.findById(event.entryId).ifPresent { entry ->
            entry.aiSummary = event.summary
            entry.aiInsights = event.keyThemes.joinToString(",")
            entry.emotionalTone = event.emotionalTone
            entry.updatedAt = System.currentTimeMillis()
            journalRepo.save(entry)
            logger.debug("AI summary saved for journal entry {}", event.entryId)
        }
    }
}
