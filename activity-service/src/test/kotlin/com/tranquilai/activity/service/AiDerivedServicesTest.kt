package com.tranquilai.activity.service

import com.tranquilai.activity.client.AiServiceClient
import com.tranquilai.activity.client.EntitlementResponse
import com.tranquilai.activity.client.JournalSummaryClientResponse
import com.tranquilai.activity.client.JournalSummaryClientRequest
import com.tranquilai.activity.client.MoodInsightClientRequest
import com.tranquilai.activity.client.SubscriptionServiceClient
import com.tranquilai.activity.dto.request.CreateJournalEntryRequest
import com.tranquilai.activity.dto.request.LogMoodRequest
import com.tranquilai.activity.entity.JournalEntry
import com.tranquilai.activity.entity.MoodEntry
import com.tranquilai.activity.messaging.ActivityEventListeners
import com.tranquilai.activity.messaging.ActivityMessaging
import com.tranquilai.activity.messaging.JournalSummaryGeneratedEvent
import com.tranquilai.activity.messaging.JournalSummaryRequestedEvent
import com.tranquilai.activity.messaging.MoodInsightGeneratedEvent
import com.tranquilai.activity.messaging.MoodInsightRequestedEvent
import com.tranquilai.activity.repository.JournalEntryRepository
import com.tranquilai.activity.repository.MoodEntryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.Optional
import java.util.UUID

class AiDerivedServicesTest {

    private val aiClient: AiServiceClient = mock(AiServiceClient::class.java)

    @Test
    fun `mood insight service publishes request event`() {
        val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
        val subscriptionClient: SubscriptionServiceClient = mock(SubscriptionServiceClient::class.java)
        val service = MoodInsightService(rabbitTemplate, subscriptionClient)
        val userId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        val request = LogMoodRequest(
            moodScore = 5,
            notes = "Tense",
            emotions = listOf("anxious"),
            stressCauses = listOf("work"),
            languageCode = "en",
        )
        `when`(subscriptionClient.checkEntitlement(userId, "ADVANCED_MOOD_INSIGHTS"))
            .thenReturn(EntitlementResponse(allowed = true, plan = "PREMIUM"))

        service.generateAndSave(userId, entryId, request)

        val eventCaptor = ArgumentCaptor.forClass(MoodInsightRequestedEvent::class.java)
        verify(rabbitTemplate).convertAndSend(
            org.mockito.Mockito.eq(ActivityMessaging.Exchange),
            org.mockito.Mockito.eq(ActivityMessaging.AiMoodInsightRoutingKey),
            eventCaptor.capture(),
        )
        assertEquals(entryId, eventCaptor.value.entryId)
        assertEquals(5, eventCaptor.value.moodScore)
        assertEquals(listOf("anxious"), eventCaptor.value.emotions)
    }

    @Test
    fun `mood insight service does not publish when entitlement is denied`() {
        val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
        val subscriptionClient: SubscriptionServiceClient = mock(SubscriptionServiceClient::class.java)
        val service = MoodInsightService(rabbitTemplate, subscriptionClient)
        val userId = UUID.randomUUID()
        `when`(subscriptionClient.checkEntitlement(userId, "ADVANCED_MOOD_INSIGHTS"))
            .thenReturn(EntitlementResponse(allowed = false, plan = "FREE"))

        service.generateAndSave(userId, UUID.randomUUID(), LogMoodRequest(moodScore = 5))

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyObject())
    }

    @Test
    fun `mood insight listener publishes generated insight result when client returns one`() {
        val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
        val service = listeners(rabbitTemplate = rabbitTemplate)
        val entryId = UUID.randomUUID()
        `when`(aiClient.generateMoodInsight(anyMoodInsightRequest())).thenReturn("Breathe slowly.")

        service.handleMoodInsightEvent(
            MoodInsightRequestedEvent(
                entryId = entryId,
                moodScore = 5,
                notes = "Tense",
                emotions = listOf("anxious"),
                stressCauses = listOf("work"),
                languageCode = "en",
            ),
        )

        val eventCaptor = ArgumentCaptor.forClass(MoodInsightGeneratedEvent::class.java)
        verify(rabbitTemplate).convertAndSend(
            org.mockito.Mockito.eq(ActivityMessaging.Exchange),
            org.mockito.Mockito.eq(ActivityMessaging.AiMoodInsightResultRoutingKey),
            eventCaptor.capture(),
        )
        assertEquals(entryId, eventCaptor.value.entryId)
        assertEquals("Breathe slowly.", eventCaptor.value.insight)
    }

    @Test
    fun `mood insight listener does nothing when AI client returns null`() {
        val repo: MoodEntryRepository = mock(MoodEntryRepository::class.java)
        val service = listeners(moodRepo = repo)
        `when`(aiClient.generateMoodInsight(anyMoodInsightRequest())).thenReturn(null)

        service.handleMoodInsightEvent(MoodInsightRequestedEvent(entryId = UUID.randomUUID(), moodScore = 5))

        verify(repo, never()).findById(anyUuid())
    }

    @Test
    fun `mood insight result listener saves generated insight`() {
        val repo: MoodEntryRepository = mock(MoodEntryRepository::class.java)
        val service = listeners(moodRepo = repo)
        val entry = MoodEntry(userId = UUID.randomUUID(), moodScore = 5)
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))
        `when`(repo.save(entry)).thenReturn(entry)

        service.handleMoodInsightResultEvent(MoodInsightGeneratedEvent(entry.id, "Breathe slowly."))

        assertEquals("Breathe slowly.", entry.aiInsight)
        verify(repo).save(entry)
    }

    @Test
    fun `journal summary service publishes request event`() {
        val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
        val subscriptionClient: SubscriptionServiceClient = mock(SubscriptionServiceClient::class.java)
        val service = JournalSummaryService(rabbitTemplate, subscriptionClient)
        val userId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        val request = CreateJournalEntryRequest(
            promptText = "How do you feel?",
            category = "reflection",
            content = "I felt grounded.",
            languageCode = "en",
        )
        `when`(subscriptionClient.checkEntitlement(userId, "JOURNAL_SUMMARY"))
            .thenReturn(EntitlementResponse(allowed = true, plan = "PREMIUM"))

        service.summarizeAndSave(userId, entryId, request)

        val eventCaptor = ArgumentCaptor.forClass(JournalSummaryRequestedEvent::class.java)
        verify(rabbitTemplate).convertAndSend(
            org.mockito.Mockito.eq(ActivityMessaging.Exchange),
            org.mockito.Mockito.eq(ActivityMessaging.AiJournalSummaryRoutingKey),
            eventCaptor.capture(),
        )
        assertEquals(entryId, eventCaptor.value.entryId)
        assertEquals("I felt grounded.", eventCaptor.value.content)
    }

    @Test
    fun `journal summary service does not publish when entitlement is denied`() {
        val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
        val subscriptionClient: SubscriptionServiceClient = mock(SubscriptionServiceClient::class.java)
        val service = JournalSummaryService(rabbitTemplate, subscriptionClient)
        val userId = UUID.randomUUID()
        `when`(subscriptionClient.checkEntitlement(userId, "JOURNAL_SUMMARY"))
            .thenReturn(EntitlementResponse(allowed = false, plan = "FREE"))

        service.summarizeAndSave(userId, UUID.randomUUID(), CreateJournalEntryRequest(content = "Entry"))

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyObject())
    }

    @Test
    fun `journal summary listener publishes generated summary result`() {
        val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
        val service = listeners(rabbitTemplate = rabbitTemplate)
        val entryId = UUID.randomUUID()
        `when`(aiClient.summarizeJournal(anyJournalSummaryRequest())).thenReturn(
            JournalSummaryClientResponse(
                summary = "Grounded reflection",
                keyThemes = listOf("calm", "gratitude"),
                emotionalTone = "positive",
                suggestedFollowUp = null,
            ),
        )

        service.handleJournalSummaryEvent(
            JournalSummaryRequestedEvent(
                entryId = entryId,
                promptText = "How do you feel?",
                category = "reflection",
                content = "I felt grounded.",
                languageCode = "en",
            ),
        )

        val eventCaptor = ArgumentCaptor.forClass(JournalSummaryGeneratedEvent::class.java)
        verify(rabbitTemplate).convertAndSend(
            org.mockito.Mockito.eq(ActivityMessaging.Exchange),
            org.mockito.Mockito.eq(ActivityMessaging.AiJournalSummaryResultRoutingKey),
            eventCaptor.capture(),
        )
        assertEquals(entryId, eventCaptor.value.entryId)
        assertEquals("Grounded reflection", eventCaptor.value.summary)
        assertEquals(listOf("calm", "gratitude"), eventCaptor.value.keyThemes)
        assertEquals("positive", eventCaptor.value.emotionalTone)
    }

    @Test
    fun `journal summary result listener saves summary themes and tone`() {
        val repo: JournalEntryRepository = mock(JournalEntryRepository::class.java)
        val service = listeners(journalRepo = repo)
        val entry = JournalEntry(userId = UUID.randomUUID(), content = "I felt grounded.")
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))
        `when`(repo.save(entry)).thenReturn(entry)

        service.handleJournalSummaryResultEvent(
            JournalSummaryGeneratedEvent(
                entryId = entry.id,
                summary = "Grounded reflection",
                keyThemes = listOf("calm", "gratitude"),
                emotionalTone = "positive",
            ),
        )

        assertEquals("Grounded reflection", entry.aiSummary)
        assertEquals("calm,gratitude", entry.aiInsights)
        assertEquals("positive", entry.emotionalTone)
        verify(repo).save(entry)
    }

    @Test
    fun `journal summary listener does nothing when AI client returns null`() {
        val repo: JournalEntryRepository = mock(JournalEntryRepository::class.java)
        val service = listeners(journalRepo = repo)
        `when`(aiClient.summarizeJournal(anyJournalSummaryRequest())).thenReturn(null)

        service.handleJournalSummaryEvent(
            JournalSummaryRequestedEvent(entryId = UUID.randomUUID(), content = "Entry"),
        )

        verify(repo, never()).findById(anyUuid())
    }

    private fun listeners(
        moodRepo: MoodEntryRepository = mock(MoodEntryRepository::class.java),
        journalRepo: JournalEntryRepository = mock(JournalEntryRepository::class.java),
        rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java),
    ) = ActivityEventListeners(
        progressClient = mock(com.tranquilai.activity.client.ProgressServiceClient::class.java),
        planClient = mock(com.tranquilai.activity.client.PlanActivityClient::class.java),
        aiClient = aiClient,
        moodRepo = moodRepo,
        journalRepo = journalRepo,
        rabbitTemplate = rabbitTemplate,
    )

    private fun anyMoodInsightRequest(): MoodInsightClientRequest {
        any(MoodInsightClientRequest::class.java)
        return uninitialized()
    }

    private fun anyJournalSummaryRequest(): JournalSummaryClientRequest {
        any(JournalSummaryClientRequest::class.java)
        return uninitialized()
    }

    private fun anyUuid(): UUID {
        any(UUID::class.java)
        return uninitialized()
    }

    private fun anyString(): String {
        any(String::class.java)
        return uninitialized()
    }

    private fun anyObject(): Any {
        any(Any::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
