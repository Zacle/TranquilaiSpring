package com.tranquilai.activity.dto.request

import jakarta.validation.constraints.NotBlank

data class CreateJournalEntryRequest(
    val promptId: String? = null,
    val promptText: String? = null,
    val category: String? = null,

    @field:NotBlank
    val content: String,

    val mood: Int? = null,
    val languageCode: String = "en",
)
