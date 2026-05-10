package com.tranquilai.activity.dto.response

import java.util.UUID

data class AffirmationViewResponse(
    val id: UUID,
    val userId: UUID,
    val affirmationId: String,
    val viewedAt: Long,
)
