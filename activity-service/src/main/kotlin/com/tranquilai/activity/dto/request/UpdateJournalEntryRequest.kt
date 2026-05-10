package com.tranquilai.activity.dto.request

data class UpdateJournalEntryRequest(
    val content: String? = null,
    val mood: Int? = null,
)
