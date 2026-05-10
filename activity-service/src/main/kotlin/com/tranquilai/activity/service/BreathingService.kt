package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.LogBreathingSessionRequest
import com.tranquilai.activity.dto.response.BreathingSessionResponse
import com.tranquilai.activity.dto.response.PageResponse
import com.tranquilai.activity.entity.BreathingSession
import com.tranquilai.activity.repository.BreathingSessionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class BreathingService(
    private val repo: BreathingSessionRepository,
    private val progressService: ActivityProgressService,
    private val planService: ActivityPlanService,
) {

    fun log(userId: UUID, request: LogBreathingSessionRequest): BreathingSessionResponse {
        val session = BreathingSession(
            userId = userId,
            exerciseId = request.exerciseId,
            exerciseTitle = request.exerciseTitle,
            selectedDurationSeconds = request.selectedDurationSeconds,
            actualDurationSeconds = request.actualDurationSeconds,
            completedCycles = request.completedCycles,
            completedAt = request.completedAt,
            feelingRating = request.feelingRating,
        )
        val saved = repo.save(session)
        // Fire-and-forget background tasks
        progressService.onBreathingLogged(userId, request.actualDurationSeconds)
        planService.onBreathingLogged(userId)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID, page: Int, size: Int): PageResponse<BreathingSessionResponse> {
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
    fun history(userId: UUID, from: Long, to: Long): List<BreathingSessionResponse> =
        repo.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, from, to).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun completedCount(userId: UUID): Long = repo.countByUserId(userId)
}

fun BreathingSession.toResponse() = BreathingSessionResponse(
    id = id,
    userId = userId,
    exerciseId = exerciseId,
    exerciseTitle = exerciseTitle,
    selectedDurationSeconds = selectedDurationSeconds,
    actualDurationSeconds = actualDurationSeconds,
    completedCycles = completedCycles,
    completedAt = completedAt,
    feelingRating = feelingRating,
    createdAt = createdAt,
)
