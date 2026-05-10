package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.CreateJournalEntryRequest
import com.tranquilai.activity.dto.request.UpdateJournalEntryRequest
import com.tranquilai.activity.entity.JournalEntry
import com.tranquilai.activity.exception.ResourceNotFoundException
import com.tranquilai.activity.repository.JournalEntryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class JournalServiceTest {

    private val repo: JournalEntryRepository = mock(JournalEntryRepository::class.java)
    private val summaryService: JournalSummaryService = mock(JournalSummaryService::class.java)
    private val progressService: ActivityProgressService = mock(ActivityProgressService::class.java)
    private val planService: ActivityPlanService = mock(ActivityPlanService::class.java)
    private val service = JournalService(repo, summaryService, progressService, planService)

    @Test
    fun `create saves journal entry and triggers summary progress and plan updates`() {
        val userId = UUID.randomUUID()
        val request = CreateJournalEntryRequest(
            promptId = "prompt-1",
            promptText = "What went well?",
            category = "gratitude",
            content = "I slept well.",
            mood = 8,
        )
        `when`(repo.save(anyJournal())).thenAnswer { it.getArgument<JournalEntry>(0) }

        val response = service.create(userId, request)

        assertEquals(userId, response.userId)
        assertEquals("prompt-1", response.promptId)
        assertEquals("I slept well.", response.content)
        verify(summaryService).summarizeAndSave(response.id, request)
        verify(progressService).onJournalCreated(userId)
        verify(planService).onJournalCreated(userId)
    }

    @Test
    fun `get throws when entry belongs to another user`() {
        val entry = journal(userId = UUID.randomUUID())
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))

        assertThrows(ResourceNotFoundException::class.java) {
            service.get(UUID.randomUUID(), entry.id)
        }
    }

    @Test
    fun `update changes mutable content and mood`() {
        val userId = UUID.randomUUID()
        val entry = journal(userId = userId, content = "Old", mood = 4)
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))
        `when`(repo.save(entry)).thenReturn(entry)

        val response = service.update(userId, entry.id, UpdateJournalEntryRequest(content = "New", mood = 7))

        assertEquals("New", response.content)
        assertEquals(7, response.mood)
        verify(repo).save(entry)
    }

    @Test
    fun `toggleFavorite flips favorite state`() {
        val userId = UUID.randomUUID()
        val entry = journal(userId = userId)
        assertFalse(entry.isFavorite)
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))
        `when`(repo.save(entry)).thenReturn(entry)

        val response = service.toggleFavorite(userId, entry.id)

        assertTrue(response.isFavorite)
        verify(repo).save(entry)
    }

    @Test
    fun `delete removes owned journal entry`() {
        val userId = UUID.randomUUID()
        val entry = journal(userId = userId)
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))

        service.delete(userId, entry.id)

        verify(repo).delete(entry)
    }

    @Test
    fun `delete does not remove another user's journal entry`() {
        val entry = journal(userId = UUID.randomUUID())
        `when`(repo.findById(entry.id)).thenReturn(Optional.of(entry))

        assertThrows(ResourceNotFoundException::class.java) {
            service.delete(UUID.randomUUID(), entry.id)
        }

        verify(repo, never()).delete(anyJournal())
    }

    private fun journal(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        content: String = "Entry",
        mood: Int? = null,
    ) = JournalEntry(
        id = id,
        userId = userId,
        promptId = "prompt-1",
        promptText = "Prompt",
        category = "reflection",
        content = content,
        mood = mood,
    )

    private fun anyJournal(): JournalEntry {
        any(JournalEntry::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
