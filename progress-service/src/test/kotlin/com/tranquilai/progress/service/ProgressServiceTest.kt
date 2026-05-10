package com.tranquilai.progress.service

import com.tranquilai.progress.dto.request.UpdateStatsRequest
import com.tranquilai.progress.entity.Badge
import com.tranquilai.progress.entity.UserStats
import com.tranquilai.progress.repository.BadgeRepository
import com.tranquilai.progress.repository.UserStatsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class ProgressServiceTest {

    private val statsRepo: UserStatsRepository = mock(UserStatsRepository::class.java)
    private val badgeRepo: BadgeRepository = mock(BadgeRepository::class.java)
    private val service = ProgressService(statsRepo, badgeRepo)

    @Test
    fun `getStats creates default stats when user has none`() {
        val userId = UUID.randomUUID()
        `when`(statsRepo.findByUserId(userId)).thenReturn(Optional.empty())
        `when`(statsRepo.save(anyStats())).thenAnswer { it.getArgument<UserStats>(0) }

        val response = service.getStats(userId)

        assertEquals(userId, response.userId)
        assertEquals(0, response.totalActivitiesCompleted)
        assertEquals(0, response.totalPlansCompleted)
        verify(statsRepo).save(anyStats())
    }

    @Test
    fun `updateStats increments counters and active day fields`() {
        val userId = UUID.randomUUID()
        val stats = UserStats(userId = userId)
        `when`(statsRepo.findByUserId(userId)).thenReturn(Optional.of(stats))
        `when`(statsRepo.save(stats)).thenReturn(stats)

        val response = service.updateStats(
            userId,
            UpdateStatsRequest(
                sessionsIncrement = 2,
                minutesIncrement = 15,
                moodEntriesIncrement = 1,
                journalEntriesIncrement = 3,
                chatSessionsIncrement = 1,
                plansCompletedIncrement = 2,
                markDayActive = true,
                averageMoodScore = 7.5,
            ),
        )

        assertEquals(2, response.totalActivitiesCompleted)
        assertEquals(15, response.totalMinutesSpent)
        assertEquals(1, response.totalMoodEntries)
        assertEquals(3, response.totalJournalEntries)
        assertEquals(1, response.totalChatSessions)
        assertEquals(2, response.totalPlansCompleted)
        assertEquals(1, response.totalDaysActive)
        assertEquals(7.5f, response.averageMoodScore)
        assertNotNull(response.lastActiveDate)
    }

    @Test
    fun `updateStats updates current longest and start streak fields`() {
        val userId = UUID.randomUUID()
        val stats = UserStats(userId = userId, currentStreakDays = 2, longestStreakDays = 5)
        `when`(statsRepo.findByUserId(userId)).thenReturn(Optional.of(stats))
        `when`(statsRepo.save(stats)).thenReturn(stats)

        val reset = service.updateStats(userId, UpdateStatsRequest(streakDays = 1))
        assertEquals(1, reset.currentStreak)
        assertEquals(5, reset.longestStreak)
        assertNotNull(reset.streakStartDate)

        val increased = service.updateStats(userId, UpdateStatsRequest(streakDays = 7))
        assertEquals(7, increased.currentStreak)
        assertEquals(7, increased.longestStreak)
    }

    @Test
    fun `updateStats awards all newly reached plan badges only once`() {
        val userId = UUID.randomUUID()
        val stats = UserStats(userId = userId, plansCompleted = 2)
        val savedBadges = mutableListOf<Badge>()
        `when`(statsRepo.findByUserId(userId)).thenReturn(Optional.of(stats))
        `when`(statsRepo.save(stats)).thenReturn(stats)
        `when`(badgeRepo.existsByUserIdAndBadgeType(userId, "MINDFULNESS_MASTER")).thenReturn(true)
        `when`(badgeRepo.existsByUserIdAndBadgeType(userId, "SERENITY_SEEKER")).thenReturn(false)
        `when`(badgeRepo.save(anyBadge())).thenAnswer {
            val badge = it.getArgument<Badge>(0)
            savedBadges += badge
            badge
        }

        val response = service.updateStats(userId, UpdateStatsRequest(plansCompletedIncrement = 1))

        assertEquals(3, response.totalPlansCompleted)
        assertEquals(listOf("SERENITY_SEEKER"), savedBadges.map { it.badgeType })
        verify(badgeRepo, never()).save(badgeType("MINDFULNESS_MASTER"))
    }

    @Test
    fun `getSummary returns badges recent badges and next badge progress`() {
        val userId = UUID.randomUUID()
        val stats = UserStats(userId = userId, plansCompleted = 4)
        val badges = listOf(
            badge(userId, "SERENITY_SEEKER", "Serenity Seeker", 300),
            badge(userId, "MINDFULNESS_MASTER", "Mindfulness Master", 200),
            badge(userId, "OLDER", "Older", 100),
            badge(userId, "OLDEST", "Oldest", 50),
        )
        `when`(statsRepo.findByUserId(userId)).thenReturn(Optional.of(stats))
        `when`(badgeRepo.findByUserIdOrderByAwardedAtDesc(userId)).thenReturn(badges)

        val summary = service.getSummary(userId)

        assertEquals(4, summary.badges.size)
        assertEquals(3, summary.recentBadges.size)
        assertEquals("RESILIENCE_ROCKSTAR", summary.nextBadge?.badgeType)
        assertEquals(4, summary.nextBadge?.plansCompleted)
        assertEquals(0.25f, summary.nextBadge?.progressPercent)
    }

    @Test
    fun `getSummary returns no next badge when final milestone is reached`() {
        val userId = UUID.randomUUID()
        val stats = UserStats(userId = userId, plansCompleted = 365)
        `when`(statsRepo.findByUserId(userId)).thenReturn(Optional.of(stats))
        `when`(badgeRepo.findByUserIdOrderByAwardedAtDesc(userId)).thenReturn(emptyList())

        assertNull(service.getSummary(userId).nextBadge)
    }

    @Test
    fun `getGrowthAreas scores and selects focus and strongest areas`() {
        val userId = UUID.randomUUID()
        val stats = UserStats(
            userId = userId,
            moodEntriesCount = 2,
            journalEntriesCount = 20,
            currentStreakDays = 1,
            longestStreakDays = 4,
            totalChatSessions = 8,
            plansCompleted = 30,
        )
        `when`(statsRepo.findByUserId(userId)).thenReturn(Optional.of(stats))

        val response = service.getGrowthAreas(userId)

        assertEquals("Journaling", response.strongestArea)
        assertEquals("Mood Tracking", response.focusArea)
        assertTrue(response.growthAreas.first { it.area == "Mood Tracking" }.isGrowthArea)
        assertEquals(90, response.growthAreas.first { it.area == "Wellness Practice" }.score)
    }

    @Test
    fun `getBadges maps badge colors from known badge types`() {
        val userId = UUID.randomUUID()
        `when`(badgeRepo.findByUserIdOrderByAwardedAtDesc(userId)).thenReturn(
            listOf(badge(userId, "MINDFULNESS_MASTER", "Mindfulness Master", 1)),
        )

        val badges = service.getBadges(userId)

        assertEquals("#4CAF50", badges.single().colorHex)
    }

    private fun badge(userId: UUID, type: String, name: String, awardedAt: Long) = Badge(
        userId = userId,
        badgeType = type,
        badgeName = name,
        description = "$name description",
        awardedAt = awardedAt,
    )

    private fun anyStats(): UserStats {
        any(UserStats::class.java)
        return uninitialized()
    }

    private fun anyBadge(): Badge {
        any(Badge::class.java)
        return uninitialized()
    }

    private fun badgeType(type: String): Badge {
        org.mockito.Mockito.argThat<Badge> { it.badgeType == type }
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
