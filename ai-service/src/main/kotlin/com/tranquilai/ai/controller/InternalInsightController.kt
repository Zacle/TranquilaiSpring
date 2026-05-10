package com.tranquilai.ai.controller

import com.tranquilai.ai.dto.request.MoodInsightRequest
import com.tranquilai.ai.dto.request.SummarizeJournalRequest
import com.tranquilai.ai.service.InsightService
import com.tranquilai.ai.service.JournalAiService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/internal/insights")
@PreAuthorize("hasRole('INTERNAL')")
class InternalInsightController(
    private val insightService: InsightService,
    private val journalAiService: JournalAiService,
) {

    @PostMapping("/mood")
    fun generateMoodInsight(
        @RequestBody request: MoodInsightRequest,
    ): ResponseEntity<*> = ResponseEntity.ok(insightService.generateMoodInsight(request))

    @PostMapping("/journal")
    fun summarizeJournal(
        @RequestBody request: SummarizeJournalRequest,
    ): ResponseEntity<*> = ResponseEntity.ok(journalAiService.summarizeInternal(request))
}
