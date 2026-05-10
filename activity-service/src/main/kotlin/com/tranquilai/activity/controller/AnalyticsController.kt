package com.tranquilai.activity.controller

import com.tranquilai.activity.dto.response.ActivityBreakdownResponse
import com.tranquilai.activity.dto.response.MoodChartResponse
import com.tranquilai.activity.security.GatewayUser
import com.tranquilai.activity.service.AnalyticsService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(private val analyticsService: AnalyticsService) {

    /**
     * GET /api/analytics/mood-chart?period=week|month|year
     * Returns mood scores grouped by day for the InsightsScreen chart.
     */
    @GetMapping("/mood-chart")
    fun moodChart(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam(defaultValue = "week") period: String,
    ): ResponseEntity<MoodChartResponse> =
        ResponseEntity.ok(analyticsService.getMoodChart(user.id, period))

    /**
     * GET /api/analytics/activity-breakdown?period=week|month|year
     * Returns counts per activity type for the InsightsScreen breakdown.
     */
    @GetMapping("/activity-breakdown")
    fun activityBreakdown(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam(defaultValue = "week") period: String,
    ): ResponseEntity<ActivityBreakdownResponse> =
        ResponseEntity.ok(analyticsService.getActivityBreakdown(user.id, period))
}
