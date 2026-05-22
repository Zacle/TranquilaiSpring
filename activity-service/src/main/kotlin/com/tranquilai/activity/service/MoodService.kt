package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.LogMoodRequest
import com.tranquilai.activity.dto.request.UpdateMoodInsightRequest
import com.tranquilai.activity.dto.request.UpdateMoodRequest
import com.tranquilai.activity.dto.response.MoodEntryResponse
import com.tranquilai.activity.dto.response.PageResponse
import com.tranquilai.activity.entity.MoodEntry
import com.tranquilai.activity.exception.ResourceNotFoundException
import com.tranquilai.activity.repository.MoodEntryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Service
@Transactional
class MoodService(
    private val repo: MoodEntryRepository,
    private val insightService: MoodInsightService,
    private val progressService: ActivityProgressService,
    private val planService: ActivityPlanService,
) {

    fun log(userId: UUID, request: LogMoodRequest): MoodEntryResponse {
        val entry = repo.save(
            MoodEntry(
                userId = userId,
                moodScore = request.moodScore,
                moodLabel = request.moodLabel,
                notes = request.notes,
                factors = request.factors.joinToString(",").ifEmpty { null },
                emotions = request.emotions.joinToString(",").ifEmpty { null },
            )
        )
        // Fire-and-forget background tasks
        insightService.generateAndSave(entry.id, request)
        progressService.onMoodLogged(userId)
        planService.onMoodLogged(userId)
        return entry.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID, page: Int, size: Int): PageResponse<MoodEntryResponse> {
        val result = repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
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
    fun get(userId: UUID, id: UUID): MoodEntryResponse {
        val entry = repo.findById(id).orElseThrow { ResourceNotFoundException("Mood entry $id not found") }
        if (entry.userId != userId) throw ResourceNotFoundException("Mood entry $id not found")
        return entry.toResponse()
    }

    @Transactional(readOnly = true)
    fun history(userId: UUID, from: Long, to: Long): List<MoodEntryResponse> =
        repo.findByUserIdAndDateRange(userId, from, to).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun today(userId: UUID): MoodEntryResponse? {
        val now = Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
        val startOfDay = now.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDay = now.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return repo.findFirstByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startOfDay, endOfDay)
            ?.toResponse()
    }

    fun update(userId: UUID, id: UUID, request: UpdateMoodRequest): MoodEntryResponse {
        val entry = repo.findById(id).orElseThrow { ResourceNotFoundException("Mood entry $id not found") }
        if (entry.userId != userId) throw ResourceNotFoundException("Mood entry $id not found")
        request.moodScore?.let { entry.moodScore = it }
        request.moodLabel?.let { entry.moodLabel = it }
        request.notes?.let { entry.notes = it }
        request.factors?.let { entry.factors = it.joinToString(",").ifEmpty { null } }
        return repo.save(entry).toResponse()
    }

    fun updateInsight(userId: UUID, id: UUID, request: UpdateMoodInsightRequest): MoodEntryResponse {
        val entry = repo.findById(id).orElseThrow { ResourceNotFoundException("Mood entry $id not found") }
        if (entry.userId != userId) throw ResourceNotFoundException("Mood entry $id not found")
        entry.aiInsight = request.insight
        return repo.save(entry).toResponse()
    }

    fun delete(userId: UUID, id: UUID) {
        val entry = repo.findById(id).orElseThrow { ResourceNotFoundException("Mood entry $id not found") }
        if (entry.userId != userId) throw ResourceNotFoundException("Mood entry $id not found")
        repo.delete(entry)
    }
}

fun MoodEntry.toResponse() = MoodEntryResponse(
    id = id,
    userId = userId,
    moodScore = moodScore,
    moodLabel = moodLabel,
    notes = notes,
    factors = factors,
    emotions = emotions?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
    aiInsight = aiInsight,
    createdAt = createdAt,
)
