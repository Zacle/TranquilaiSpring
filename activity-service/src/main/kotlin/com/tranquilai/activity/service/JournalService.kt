package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.CreateJournalEntryRequest
import com.tranquilai.activity.dto.request.UpdateJournalEntryRequest
import com.tranquilai.activity.dto.response.JournalEntryResponse
import com.tranquilai.activity.dto.response.PageResponse
import com.tranquilai.activity.entity.JournalEntry
import com.tranquilai.activity.exception.ResourceNotFoundException
import com.tranquilai.activity.repository.JournalEntryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class JournalService(
    private val repo: JournalEntryRepository,
    private val journalSummaryService: JournalSummaryService,
    private val progressService: ActivityProgressService,
    private val planService: ActivityPlanService,
) {

    fun create(userId: UUID, request: CreateJournalEntryRequest): JournalEntryResponse {
        val entry = JournalEntry(
            userId = userId,
            promptId = request.promptId,
            promptText = request.promptText,
            category = request.category,
            content = request.content,
            mood = request.mood,
        )
        val saved = repo.save(entry)
        // Fire-and-forget background tasks
        journalSummaryService.summarizeAndSave(saved.id, request)
        progressService.onJournalCreated(userId)
        planService.onJournalCreated(userId)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID, page: Int, size: Int, category: String?): PageResponse<JournalEntryResponse> {
        val result = if (category != null) {
            repo.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category, PageRequest.of(page, size))
        } else {
            repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
        }
        return PageResponse(
            content = result.content.map { it.toResponse() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            last = result.isLast,
        )
    }

    @Transactional(readOnly = true)
    fun get(userId: UUID, id: UUID): JournalEntryResponse {
        val entry = repo.findById(id).orElseThrow { ResourceNotFoundException("Journal entry $id not found") }
        if (entry.userId != userId) throw ResourceNotFoundException("Journal entry $id not found")
        return entry.toResponse()
    }

    @Transactional(readOnly = true)
    fun favorites(userId: UUID): List<JournalEntryResponse> =
        repo.findByUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(userId).map { it.toResponse() }

    fun update(userId: UUID, id: UUID, request: UpdateJournalEntryRequest): JournalEntryResponse {
        val entry = repo.findById(id).orElseThrow { ResourceNotFoundException("Journal entry $id not found") }
        if (entry.userId != userId) throw ResourceNotFoundException("Journal entry $id not found")

        request.content?.let { entry.content = it }
        request.mood?.let { entry.mood = it }
        entry.updatedAt = System.currentTimeMillis()
        return repo.save(entry).toResponse()
    }

    fun toggleFavorite(userId: UUID, id: UUID): JournalEntryResponse {
        val entry = repo.findById(id).orElseThrow { ResourceNotFoundException("Journal entry $id not found") }
        if (entry.userId != userId) throw ResourceNotFoundException("Journal entry $id not found")
        entry.isFavorite = !entry.isFavorite
        entry.updatedAt = System.currentTimeMillis()
        return repo.save(entry).toResponse()
    }

    fun delete(userId: UUID, id: UUID) {
        val entry = repo.findById(id).orElseThrow { ResourceNotFoundException("Journal entry $id not found") }
        if (entry.userId != userId) throw ResourceNotFoundException("Journal entry $id not found")
        repo.delete(entry)
    }
}

fun JournalEntry.toResponse() = JournalEntryResponse(
    id = id,
    userId = userId,
    promptId = promptId,
    promptText = promptText,
    category = category,
    content = content,
    mood = mood,
    isFavorite = isFavorite,
    aiSummary = aiSummary,
    aiInsights = aiInsights?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    emotionalTone = emotionalTone,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
