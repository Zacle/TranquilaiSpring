package com.tranquilai.notification.controller

import com.tranquilai.notification.dto.response.NotificationLogResponse
import com.tranquilai.notification.dto.response.PageResponse
import com.tranquilai.notification.dto.response.ReminderScheduleResponse
import com.tranquilai.notification.security.GatewayUser
import com.tranquilai.notification.service.NotificationHistoryService
import com.tranquilai.notification.service.ReminderScheduleService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val historyService: NotificationHistoryService,
    private val scheduleService: ReminderScheduleService,
) {

    /**
     * GET /api/notifications/history
     * Paginated log of notifications sent to the current user.
     */
    @GetMapping("/history")
    fun history(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<NotificationLogResponse>> =
        ResponseEntity.ok(historyService.getHistory(user.id, page, size))

    /**
     * GET /api/notifications/reminder-schedule
     * Returns the current reminder schedule for the authenticated user.
     */
    @GetMapping("/reminder-schedule")
    fun reminderSchedule(
        @AuthenticationPrincipal user: GatewayUser,
    ): ResponseEntity<ReminderScheduleResponse> {
        val schedule = scheduleService.get(user.id)
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(schedule)
    }
}
