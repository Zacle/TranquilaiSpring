package com.tranquilai.ai.controller

import com.tranquilai.ai.dto.request.GenerateAffirmationRequest
import com.tranquilai.ai.dto.request.GenerateGreetingRequest
import com.tranquilai.ai.dto.request.MoodInsightRequest
import com.tranquilai.ai.service.InsightService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/insights")
class InsightController(private val insightService: InsightService) {

    /**
     * Stateless greeting — no conversation or DB record is created.
     * Mobile calls this to display an opening greeting, then creates the
     * conversation only when the user sends their first message.
     */
    @PostMapping("/greeting")
    fun generateGreeting(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: GenerateGreetingRequest,
    ) = ResponseEntity.ok(insightService.generateGreeting(request))

    @PostMapping("/mood")
    fun generateMoodInsight(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: MoodInsightRequest,
    ) = ResponseEntity.ok(insightService.generateMoodInsight(request))

    @PostMapping("/affirmation")
    fun generateAffirmation(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: GenerateAffirmationRequest,
    ) = ResponseEntity.ok(insightService.generateAffirmation(request))
}
