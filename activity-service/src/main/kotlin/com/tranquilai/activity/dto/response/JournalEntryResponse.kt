package com.tranquilai.activity.dto.response

import java.util.UUID

data class JournalEntryResponse(
    val id: UUID,
    val userId: UUID,
    val promptId: String?,
    val promptText: String?,
    val category: String?,
    val content: String,
    val mood: Int?,
    val isFavorite: Boolean,
    val aiSummary: String?,
    val aiInsights: List<String>,
    val emotionalTone: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
