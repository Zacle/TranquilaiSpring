package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.LogMeditationSessionRequest
import com.tranquilai.activity.dto.response.MeditationSessionResponse
import com.tranquilai.activity.dto.response.PageResponse
import com.tranquilai.activity.entity.MeditationSession
import com.tranquilai.activity.repository.MeditationSessionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class MeditationService(
    private val repo: MeditationSessionRepository,
    private val progressService: ActivityProgressService,
    private val planService: ActivityPlanService,
) {

    fun log(userId: UUID, request: LogMeditationSessionRequest): MeditationSessionResponse {
        val session = MeditationSession(
            userId = userId,
            topicId = request.topicId,
            meditationTitle = request.meditationTitle,
            durationSeconds = request.durationSeconds,
            actualDurationSeconds = request.actualDurationSeconds,
            completedAt = request.completedAt,
            feelingRating = request.feelingRating,
            soundsUsed = request.soundsUsed.joinToString(",").ifEmpty { null },
        )
        val saved = repo.save(session)
        // Fire-and-forget background tasks
        progressService.onMeditationLogged(userId, request.actualDurationSeconds)
        planService.onMeditationLogged(userId)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID, page: Int, size: Int): PageResponse<MeditationSessionResponse> {
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
    fun history(userId: UUID, from: Long, to: Long): List<MeditationSessionResponse> =
        repo.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, from, to).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun completedCount(userId: UUID): Long = repo.countByUserId(userId)
}

fun MeditationSession.toResponse() = MeditationSessionResponse(
    id = id,
    userId = userId,
    topicId = topicId,
    meditationTitle = meditationTitle,
    durationSeconds = durationSeconds,
    actualDurationSeconds = actualDurationSeconds,
    completedAt = completedAt,
    feelingRating = feelingRating,
    soundsUsed = soundsUsed?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    createdAt = createdAt,
)
