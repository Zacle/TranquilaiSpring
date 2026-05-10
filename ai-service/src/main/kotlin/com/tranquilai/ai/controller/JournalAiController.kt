package com.tranquilai.ai.controller

import com.tranquilai.ai.dto.request.SummarizeJournalRequest
import com.tranquilai.ai.service.JournalAiService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/ai/journal")
class JournalAiController(private val journalAiService: JournalAiService) {

    @PostMapping("/summarize")
    fun summarize(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: SummarizeJournalRequest,
    ) = ResponseEntity.ok(journalAiService.summarize(userId, request))
}
