package com.tranquilai.activity.controller

import com.tranquilai.activity.dto.request.CreateJournalEntryRequest
import com.tranquilai.activity.dto.request.LogAffirmationViewRequest
import com.tranquilai.activity.dto.request.LogBreathingSessionRequest
import com.tranquilai.activity.dto.request.LogMeditationSessionRequest
import com.tranquilai.activity.dto.request.LogMoodRequest
import com.tranquilai.activity.dto.request.UpdateJournalEntryRequest
import com.tranquilai.activity.dto.request.UpdateMoodInsightRequest
import com.tranquilai.activity.dto.request.UpdateMoodRequest
import com.tranquilai.activity.dto.response.ActivityBreakdownResponse
import com.tranquilai.activity.dto.response.AffirmationViewResponse
import com.tranquilai.activity.dto.response.BreathingSessionResponse
import com.tranquilai.activity.dto.response.JournalEntryResponse
import com.tranquilai.activity.dto.response.MeditationSessionResponse
import com.tranquilai.activity.dto.response.MoodChartResponse
import com.tranquilai.activity.dto.response.MoodEntryResponse
import com.tranquilai.activity.dto.response.PageResponse
import com.tranquilai.activity.security.GatewayUser
import com.tranquilai.activity.service.AffirmationViewService
import com.tranquilai.activity.service.AnalyticsService
import com.tranquilai.activity.service.BreathingService
import com.tranquilai.activity.service.JournalService
import com.tranquilai.activity.service.MeditationService
import com.tranquilai.activity.service.MoodService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import java.util.UUID

class ActivityControllersTest {

    private val user = GatewayUser(UUID.randomUUID(), "user@example.com", "USER")

    @Test
    fun `mood controller delegates create read update and delete operations`() {
        val service: MoodService = mock(MoodService::class.java)
        val controller = MoodController(service)
        val id = UUID.randomUUID()
        val response = moodResponse(id)
        val pageResponse = PageResponse(listOf(response), 0, 20, 1, 1, true)
        val logRequest = LogMoodRequest(moodScore = 7)
        val updateRequest = UpdateMoodRequest(notes = "updated")
        val insightRequest = UpdateMoodInsightRequest("insight")
        `when`(service.log(user.id, logRequest)).thenReturn(response)
        `when`(service.list(user.id, 0, 20)).thenReturn(pageResponse)
        `when`(service.get(user.id, id)).thenReturn(response)
        `when`(service.history(user.id, 1, 2)).thenReturn(listOf(response))
        `when`(service.today(user.id)).thenReturn(response)
        `when`(service.update(user.id, id, updateRequest)).thenReturn(response)
        `when`(service.updateInsight(user.id, id, insightRequest)).thenReturn(response)

        assertEquals(HttpStatus.CREATED, controller.log(user, logRequest).statusCode)
        assertEquals(pageResponse, controller.list(user, 0, 20).body)
        assertEquals(response, controller.get(user, id).body)
        assertEquals(listOf(response), controller.history(user, 1, 2).body)
        assertEquals(response, controller.today(user).body)
        assertEquals(response, controller.update(user, id, updateRequest).body)
        assertEquals(response, controller.updateInsight(user, id, insightRequest).body)
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(user, id).statusCode)
        verify(service).delete(user.id, id)
    }

    @Test
    fun `mood today returns no content when there is no entry`() {
        val service: MoodService = mock(MoodService::class.java)
        `when`(service.today(user.id)).thenReturn(null)

        assertEquals(HttpStatus.NO_CONTENT, MoodController(service).today(user).statusCode)
    }

    @Test
    fun `journal controller delegates main journal operations`() {
        val service: JournalService = mock(JournalService::class.java)
        val controller = JournalController(service)
        val id = UUID.randomUUID()
        val response = journalResponse(id)
        val pageResponse = PageResponse(listOf(response), 0, 20, 1, 1, true)
        val createRequest = CreateJournalEntryRequest(content = "entry")
        val updateRequest = UpdateJournalEntryRequest(content = "updated")
        `when`(service.create(user.id, createRequest)).thenReturn(response)
        `when`(service.list(user.id, 0, 20, "reflection")).thenReturn(pageResponse)
        `when`(service.favorites(user.id)).thenReturn(listOf(response))
        `when`(service.get(user.id, id)).thenReturn(response)
        `when`(service.update(user.id, id, updateRequest)).thenReturn(response)
        `when`(service.toggleFavorite(user.id, id)).thenReturn(response)

        assertEquals(HttpStatus.CREATED, controller.create(user, createRequest).statusCode)
        assertEquals(pageResponse, controller.list(user, 0, 20, "reflection").body)
        assertEquals(listOf(response), controller.favorites(user).body)
        assertEquals(response, controller.get(user, id).body)
        assertEquals(response, controller.update(user, id, updateRequest).body)
        assertEquals(response, controller.toggleFavorite(user, id).body)
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(user, id).statusCode)
        verify(service).delete(user.id, id)
    }

    @Test
    fun `session and analytics controllers delegate to services`() {
        val meditationService: MeditationService = mock(MeditationService::class.java)
        val breathingService: BreathingService = mock(BreathingService::class.java)
        val affirmationService: AffirmationViewService = mock(AffirmationViewService::class.java)
        val analyticsService: AnalyticsService = mock(AnalyticsService::class.java)
        val meditation = meditationResponse()
        val breathing = breathingResponse()
        val affirmation = affirmationResponse()
        val meditationRequest = LogMeditationSessionRequest("topic", "title", 60, 60, 1000)
        val breathingRequest = LogBreathingSessionRequest("exercise", "title", 60, 60, 4, 1000)
        val affirmationRequest = LogAffirmationViewRequest("self_worth")
        `when`(meditationService.log(user.id, meditationRequest)).thenReturn(meditation)
        `when`(meditationService.completedCount(user.id)).thenReturn(4)
        `when`(breathingService.log(user.id, breathingRequest)).thenReturn(breathing)
        `when`(breathingService.completedCount(user.id)).thenReturn(5)
        `when`(affirmationService.log(user.id, affirmationRequest)).thenReturn(affirmation)
        `when`(analyticsService.getMoodChart(user.id, "week")).thenReturn(MoodChartResponse("week", emptyList(), false, 3))
        `when`(analyticsService.getActivityBreakdown(user.id, "week")).thenReturn(ActivityBreakdownResponse("week", 1, 2, 1, 1, 1, 1, 4))

        assertEquals(HttpStatus.CREATED, MeditationController(meditationService).log(user, meditationRequest).statusCode)
        assertEquals(mapOf("completedCount" to 4L), MeditationController(meditationService).stats(user).body)
        assertEquals(HttpStatus.CREATED, BreathingController(breathingService).log(user, breathingRequest).statusCode)
        assertEquals(mapOf("completedCount" to 5L), BreathingController(breathingService).stats(user).body)
        assertEquals(HttpStatus.CREATED, AffirmationViewController(affirmationService).log(user, affirmationRequest).statusCode)
        assertEquals("week", AnalyticsController(analyticsService).moodChart(user, "week").body?.period)
        assertEquals(4, AnalyticsController(analyticsService).activityBreakdown(user, "week").body?.total)
    }

    private fun moodResponse(id: UUID = UUID.randomUUID()) = MoodEntryResponse(
        id = id,
        userId = user.id,
        moodScore = 7,
        moodLabel = "Calm",
        notes = null,
        factors = null,
        aiInsight = null,
        createdAt = 1,
    )

    private fun journalResponse(id: UUID = UUID.randomUUID()) = JournalEntryResponse(
        id = id,
        userId = user.id,
        promptId = null,
        promptText = null,
        category = null,
        content = "entry",
        mood = null,
        isFavorite = false,
        aiSummary = null,
        aiInsights = emptyList(),
        emotionalTone = null,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun meditationResponse() = MeditationSessionResponse(
        id = UUID.randomUUID(),
        userId = user.id,
        topicId = "topic",
        meditationTitle = "title",
        durationSeconds = 60,
        actualDurationSeconds = 60,
        completedAt = 1000,
        feelingRating = null,
        soundsUsed = emptyList(),
        createdAt = 1,
    )

    private fun breathingResponse() = BreathingSessionResponse(
        id = UUID.randomUUID(),
        userId = user.id,
        exerciseId = "exercise",
        exerciseTitle = "title",
        selectedDurationSeconds = 60,
        actualDurationSeconds = 60,
        completedCycles = 4,
        completedAt = 1000,
        feelingRating = null,
        createdAt = 1,
    )

    private fun affirmationResponse() = AffirmationViewResponse(
        id = UUID.randomUUID(),
        userId = user.id,
        affirmationId = "self_worth",
        viewedAt = 1,
    )
}
