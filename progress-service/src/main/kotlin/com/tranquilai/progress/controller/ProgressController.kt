package com.tranquilai.progress.controller

import com.tranquilai.progress.dto.request.UpdateStatsRequest
import com.tranquilai.progress.dto.response.BadgeResponse
import com.tranquilai.progress.dto.response.GrowthAreasResponse
import com.tranquilai.progress.dto.response.ProgressSummaryResponse
import com.tranquilai.progress.dto.response.UserStatsResponse
import com.tranquilai.progress.security.GatewayUser
import com.tranquilai.progress.service.ProgressService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/progress")
class ProgressController(private val progressService: ProgressService) {

    /** GET /api/progress/summary — full summary (stats + badges) */
    @GetMapping("/summary")
    fun summary(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<ProgressSummaryResponse> =
        ResponseEntity.ok(progressService.getSummary(user.id))

    /** GET /api/progress/stats */
    @GetMapping("/stats")
    fun stats(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<UserStatsResponse> =
        ResponseEntity.ok(progressService.getStats(user.id))

    /** PATCH /api/progress/stats — increment counters (called by other services) */
    @PatchMapping("/stats")
    fun updateStats(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestBody request: UpdateStatsRequest,
    ): ResponseEntity<UserStatsResponse> =
        ResponseEntity.ok(progressService.updateStats(user.id, request))

    /** GET /api/progress/badges */
    @GetMapping("/badges")
    fun badges(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<List<BadgeResponse>> =
        ResponseEntity.ok(progressService.getBadges(user.id))

    /** GET /api/progress/insights/growth-areas — wellness growth analysis for InsightsScreen */
    @GetMapping("/insights/growth-areas")
    fun growthAreas(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<GrowthAreasResponse> =
        ResponseEntity.ok(progressService.getGrowthAreas(user.id))
}
