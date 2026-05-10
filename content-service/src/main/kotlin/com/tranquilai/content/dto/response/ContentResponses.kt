package com.tranquilai.content.dto.response

// Kept for PageResponse usage elsewhere; content-specific DTOs removed —
// content text lives in the mobile's string resources.

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
)
