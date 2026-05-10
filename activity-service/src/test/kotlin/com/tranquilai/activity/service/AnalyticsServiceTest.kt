package com.tranquilai.activity.service

import com.tranquilai.activity.entity.MoodEntry
import com.tranquilai.activity.repository.BreathingSessionRepository
import com.tranquilai.activity.repository.JournalEntryRepository
import com.tranquilai.activity.repository.MeditationSessionRepository
import com.tranquilai.activity.repository.MoodEntryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class AnalyticsServiceTest {

    private val moodRepo: MoodEntryRepository = mock(MoodEntryRepository::class.java)
    private val journalRepo: JournalEntryRepository = mock(JournalEntryRepository::class.java)
    private val breathingRepo: BreathingSessionRepository = mock(BreathingSessionRepository::class.java)
    private val meditationRepo: MeditationSessionRepository = mock(MeditationSessionRepository::class.java)
    private val service = AnalyticsService(moodRepo, journalRepo, breathingRepo, meditationRepo)

    @Test
    fun `getMoodChart groups mood scores into week slots and reports enough data`() {
        val userId = UUID.randomUUID()
        val today = Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
        val todayNoon = today.atTime(12, 0).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        val yesterdayNoon = today.minusDays(1).atTime(12, 0).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        `when`(moodRepo.findByUserIdAndDateRange(eqUuid(userId), anyLong(), anyLong())).thenReturn(
            listOf(
                mood(userId, 8, todayNoon),
                mood(userId, 6, todayNoon + 1),
                mood(userId, 4, yesterdayNoon),
            ),
        )

        val response = service.getMoodChart(userId, "week")

        assertEquals("week", response.period)
        assertEquals(7, response.data.size)
        assertTrue(response.hasEnoughData)
        assertEquals(0, response.dataNeededCount)
        assertEquals(2, response.data.last().entryCount)
        assertEquals(7.0f, response.data.last().averageMoodScore)
    }

    @Test
    fun `getMoodChart reports data needed when entries are below period minimum`() {
        val userId = UUID.randomUUID()
        `when`(moodRepo.findByUserIdAndDateRange(eqUuid(userId), anyLong(), anyLong())).thenReturn(emptyList())

        val response = service.getMoodChart(userId, "month")

        assertFalse(response.hasEnoughData)
        assertEquals(7, response.dataNeededCount)
        assertEquals(30, response.data.size)
    }

    @Test
    fun `getActivityBreakdown returns counts and total`() {
        val userId = UUID.randomUUID()
        `when`(moodRepo.countByUserIdAndCreatedAtBetween(eqUuid(userId), anyLong(), anyLong())).thenReturn(2)
        `when`(journalRepo.countByUserIdAndCreatedAtBetween(eqUuid(userId), anyLong(), anyLong())).thenReturn(3)
        `when`(breathingRepo.countByUserIdAndCreatedAtBetween(eqUuid(userId), anyLong(), anyLong())).thenReturn(4)
        `when`(meditationRepo.countByUserIdAndCreatedAtBetween(eqUuid(userId), anyLong(), anyLong())).thenReturn(5)

        val response = service.getActivityBreakdown(userId, "week")

        assertEquals("week", response.period)
        assertEquals(2, response.moodEntries)
        assertEquals(3, response.journalEntries)
        assertEquals(4, response.breathingSessions)
        assertEquals(5, response.meditationSessions)
        assertEquals(14, response.total)
    }

    private fun mood(userId: UUID, score: Int, createdAt: Long) =
        MoodEntry(userId = userId, moodScore = score, createdAt = createdAt)

    private fun eqUuid(value: UUID): UUID {
        org.mockito.Mockito.eq(value)
        return value
    }
}
