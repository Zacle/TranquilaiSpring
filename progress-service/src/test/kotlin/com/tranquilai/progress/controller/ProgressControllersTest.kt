package com.tranquilai.progress.controller

import com.tranquilai.progress.dto.request.UpdateStatsRequest
import com.tranquilai.progress.dto.response.BadgeResponse
import com.tranquilai.progress.dto.response.GrowthArea
import com.tranquilai.progress.dto.response.GrowthAreasResponse
import com.tranquilai.progress.dto.response.ProgressSummaryResponse
import com.tranquilai.progress.dto.response.UserStatsResponse
import com.tranquilai.progress.security.GatewayUser
import com.tranquilai.progress.service.ProgressService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class ProgressControllersTest {

    private val user = GatewayUser(UUID.randomUUID(), "user@example.com", "USER")

    @Test
    fun `progress controller delegates public progress operations`() {
        val service: ProgressService = mock(ProgressService::class.java)
        val controller = ProgressController(service)
        val stats = statsResponse(user.id)
        val badge = badgeResponse(user.id)
        val summary = ProgressSummaryResponse(stats, listOf(badge), listOf(badge), null)
        val growth = GrowthAreasResponse(
            growthAreas = listOf(GrowthArea("Mood Tracking", 50, "Log mood", true)),
            strongestArea = "Mood Tracking",
            focusArea = "Mood Tracking",
        )
        val updateRequest = UpdateStatsRequest(sessionsIncrement = 1)
        `when`(service.getSummary(user.id)).thenReturn(summary)
        `when`(service.getStats(user.id)).thenReturn(stats)
        `when`(service.updateStats(user.id, updateRequest)).thenReturn(stats)
        `when`(service.getBadges(user.id)).thenReturn(listOf(badge))
        `when`(service.getGrowthAreas(user.id)).thenReturn(growth)

        assertEquals(summary, controller.summary(user).body)
        assertEquals(stats, controller.stats(user).body)
        assertEquals(stats, controller.updateStats(user, updateRequest).body)
        assertEquals(listOf(badge), controller.badges(user).body)
        assertEquals(growth, controller.growthAreas(user).body)
    }

    @Test
    fun `internal progress controller delegates stats update for path user`() {
        val service: ProgressService = mock(ProgressService::class.java)
        val userId = UUID.randomUUID()
        val request = UpdateStatsRequest(minutesIncrement = 5)
        val response = statsResponse(userId)
        `when`(service.updateStats(userId, request)).thenReturn(response)

        assertEquals(response, InternalProgressController(service).updateStats(userId, request).body)
        verify(service).updateStats(userId, request)
    }

    private fun statsResponse(userId: UUID) = UserStatsResponse(
        id = UUID.randomUUID(),
        userId = userId,
        currentStreak = 1,
        longestStreak = 2,
        totalDaysActive = 3,
        totalActivitiesCompleted = 4,
        totalMoodEntries = 5,
        totalJournalEntries = 6,
        totalChatSessions = 7,
        totalMinutesSpent = 8,
        totalPlansCompleted = 9,
        lastActiveDate = 10,
        streakStartDate = 11,
        averageMoodScore = 7.5f,
        createdAt = 12,
        updatedAt = 13,
    )

    private fun badgeResponse(userId: UUID) = BadgeResponse(
        id = UUID.randomUUID(),
        userId = userId,
        type = "MINDFULNESS_MASTER",
        badgeName = "Mindfulness Master",
        description = "Completed your first daily plan",
        colorHex = "#4CAF50",
        earnedAt = 1,
    )
}
