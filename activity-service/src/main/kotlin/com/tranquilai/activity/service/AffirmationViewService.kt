package com.tranquilai.activity.service

import com.tranquilai.activity.dto.request.LogAffirmationViewRequest
import com.tranquilai.activity.dto.response.AffirmationViewResponse
import com.tranquilai.activity.entity.AffirmationView
import com.tranquilai.activity.repository.AffirmationViewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class AffirmationViewService(
    private val repo: AffirmationViewRepository,
    private val progressService: ActivityProgressService,
    private val planService: ActivityPlanService,
) {
    fun log(userId: UUID, request: LogAffirmationViewRequest): AffirmationViewResponse {
        val view = repo.save(
            AffirmationView(
                userId = userId,
                affirmationId = request.affirmationId,
            )
        )
        progressService.onAffirmationViewed(userId)
        planService.onAffirmationViewed(userId)
        return view.toResponse()
    }
}

fun AffirmationView.toResponse() = AffirmationViewResponse(
    id = id,
    userId = userId,
    affirmationId = affirmationId,
    viewedAt = viewedAt,
)
