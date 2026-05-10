package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.LogAffirmationViewRequest
import com.tranquilai.activity.dto.request.LogBreathingSessionRequest
import com.tranquilai.activity.dto.request.LogMeditationSessionRequest
import com.tranquilai.activity.entity.AffirmationView
import com.tranquilai.activity.entity.BreathingSession
import com.tranquilai.activity.entity.MeditationSession
import com.tranquilai.activity.repository.AffirmationViewRepository
import com.tranquilai.activity.repository.BreathingSessionRepository
import com.tranquilai.activity.repository.MeditationSessionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class SessionServicesTest {

    private val progressService: ActivityProgressService = mock(ActivityProgressService::class.java)
    private val planService: ActivityPlanService = mock(ActivityPlanService::class.java)

    @Test
    fun `meditation log saves session and updates progress and plan`() {
        val repo: MeditationSessionRepository = mock(MeditationSessionRepository::class.java)
        val service = MeditationService(repo, progressService, planService)
        val userId = UUID.randomUUID()
        val request = LogMeditationSessionRequest(
            topicId = "mindfulness_calm_focus",
            meditationTitle = "Calm Focus",
            durationSeconds = 600,
            actualDurationSeconds = 540,
            completedAt = 1234,
            feelingRating = 5,
            soundsUsed = listOf("nature_rain", "sleep_fan"),
        )
        `when`(repo.save(anyMeditation())).thenAnswer { it.getArgument<MeditationSession>(0) }

        val response = service.log(userId, request)

        assertEquals("mindfulness_calm_focus", response.topicId)
        assertEquals(listOf("nature_rain", "sleep_fan"), response.soundsUsed)
        verify(progressService).onMeditationLogged(userId, 540)
        verify(planService).onMeditationLogged(userId)
    }

    @Test
    fun `breathing log saves session and updates progress and plan`() {
        val repo: BreathingSessionRepository = mock(BreathingSessionRepository::class.java)
        val service = BreathingService(repo, progressService, planService)
        val userId = UUID.randomUUID()
        val request = LogBreathingSessionRequest(
            exerciseId = "box_breathing",
            exerciseTitle = "Box Breathing",
            selectedDurationSeconds = 300,
            actualDurationSeconds = 240,
            completedCycles = 8,
            completedAt = 1234,
            feelingRating = 4,
        )
        `when`(repo.save(anyBreathing())).thenAnswer { it.getArgument<BreathingSession>(0) }

        val response = service.log(userId, request)

        assertEquals("box_breathing", response.exerciseId)
        assertEquals(8, response.completedCycles)
        verify(progressService).onBreathingLogged(userId, 240)
        verify(planService).onBreathingLogged(userId)
    }

    @Test
    fun `affirmation view log saves view and updates progress and plan`() {
        val repo: AffirmationViewRepository = mock(AffirmationViewRepository::class.java)
        val service = AffirmationViewService(repo, progressService, planService)
        val userId = UUID.randomUUID()
        `when`(repo.save(anyAffirmationView())).thenAnswer { it.getArgument<AffirmationView>(0) }

        val response = service.log(userId, LogAffirmationViewRequest("self_worth"))

        assertEquals(userId, response.userId)
        assertEquals("self_worth", response.affirmationId)
        verify(progressService).onAffirmationViewed(userId)
        verify(planService).onAffirmationViewed(userId)
    }

    private fun anyMeditation(): MeditationSession {
        any(MeditationSession::class.java)
        return uninitialized()
    }

    private fun anyBreathing(): BreathingSession {
        any(BreathingSession::class.java)
        return uninitialized()
    }

    private fun anyAffirmationView(): AffirmationView {
        any(AffirmationView::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
