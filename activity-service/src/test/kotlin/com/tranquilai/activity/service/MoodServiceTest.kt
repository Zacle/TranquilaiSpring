package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.LogMoodRequest
import com.tranquilai.activity.dto.request.UpdateMoodInsightRequest
import com.tranquilai.activity.dto.request.UpdateMoodRequest
import com.tranquilai.activity.entity.MoodEntry
import com.tranquilai.activity.exception.ResourceNotFoundException
import com.tranquilai.activity.repository.MoodEntryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class MoodServiceTest {

    private val repo: MoodEntryRepository = mock(MoodEntryRepository::class.java)
    private val insightService: MoodInsightService = mock(MoodInsightService::class.java)
    private val progressService: ActivityProgressService = mock(ActivityProgressService::class.java)
    private val planService: ActivityPlanService = mock(ActivityPlanService::class.java)
    private val service = MoodService(repo, insightService, progressService, planService)

    @Test
    fun `log saves mood and triggers insight progress and plan updates`() {
        val userId = UUID.randomUUID()
        val request = LogMoodRequest(
            moodScore = 8,
            moodLabel = "Calm",
            notes = "Good day",
            factors = listOf("sleep", "exercise"),
            emotions = listOf("relaxed"),
            stressCauses = listOf("work"),
        )
        `when`(repo.save(anyMood())).thenAnswer { it.getArgument<MoodEntry>(0) }

        val response = service.log(userId, request)

        assertEquals(userId, response.userId)
        assertEquals(8, response.moodScore)
        assertEquals("sleep,exercise", response.factors)
        verify(insightService).generateAndSave(response.id, request)
        verify(progressService).onMoodLogged(userId)
        verify(planService).onMoodLogged(userId)
    }

    @Test
    fun `get returns only entries owned by requesting user`() {
        val userId = UUID.randomUUID()
        val entry = mood(userId = userId)
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))

        val response = service.get(userId, entry.id)

        assertEquals(entry.id, response.id)
        assertEquals(userId, response.userId)
    }

    @Test
    fun `get throws when entry belongs to another user`() {
        val userId = UUID.randomUUID()
        val entry = mood(userId = UUID.randomUUID())
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))

        assertThrows(ResourceNotFoundException::class.java) {
            service.get(userId, entry.id)
        }
    }

    @Test
    fun `update applies provided fields and persists`() {
        val userId = UUID.randomUUID()
        val entry = mood(userId = userId, moodScore = 4, moodLabel = "Low", factors = "work")
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))
        `when`(repo.save(entry)).thenReturn(entry)

        val response = service.update(
            userId,
            entry.id,
            UpdateMoodRequest(
                moodScore = 7,
                notes = "Recovered",
                factors = listOf("walk", "breathing"),
            ),
        )

        assertEquals(7, response.moodScore)
        assertEquals("Low", response.moodLabel)
        assertEquals("Recovered", response.notes)
        assertEquals("walk,breathing", response.factors)
        verify(repo).save(entry)
    }

    @Test
    fun `updateInsight saves generated insight`() {
        val userId = UUID.randomUUID()
        val entry = mood(userId = userId)
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))
        `when`(repo.save(entry)).thenReturn(entry)

        val response = service.updateInsight(userId, entry.id, UpdateMoodInsightRequest("Try a short walk."))

        assertEquals("Try a short walk.", response.aiInsight)
        verify(repo).save(entry)
    }

    @Test
    fun `delete removes owned entry`() {
        val userId = UUID.randomUUID()
        val entry = mood(userId = userId)
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))

        service.delete(userId, entry.id)

        verify(repo).delete(entry)
    }

    @Test
    fun `delete does not remove another user's entry`() {
        val entry = mood(userId = UUID.randomUUID())
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))

        assertThrows(ResourceNotFoundException::class.java) {
            service.delete(UUID.randomUUID(), entry.id)
        }

        verify(repo, never()).delete(anyMood())
    }

    private fun mood(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        moodScore: Int = 5,
        moodLabel: String? = "Okay",
        factors: String? = null,
    ) = MoodEntry(
        id = id,
        userId = userId,
        moodScore = moodScore,
        moodLabel = moodLabel,
        factors = factors,
    )

    private fun anyMood(): MoodEntry {
        any(MoodEntry::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
