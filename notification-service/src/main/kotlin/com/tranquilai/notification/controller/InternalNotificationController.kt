package com.tranquilai.notification.controller

import com.tranquilai.notification.dto.request.SendNotificationRequest
import com.tranquilai.notification.dto.request.UpsertReminderScheduleRequest
import com.tranquilai.notification.dto.response.ReminderScheduleResponse
import com.tranquilai.notification.dto.response.SendNotificationResult
import com.tranquilai.notification.service.FcmService
import com.tranquilai.notification.service.NotificationAccountDeletionService
import com.tranquilai.notification.service.PushPayload
import com.tranquilai.notification.service.ReminderScheduleService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Internal endpoints — accessible only from other backend services via X-Internal-Key header.
 * Used for inter-service communication (e.g. user-service calls upsertReminderSchedule when user updates settings).
 */
@RestController
@RequestMapping("/internal/notifications")
class InternalNotificationController(
    private val fcmService: FcmService,
    private val scheduleService: ReminderScheduleService,
    private val accountDeletionService: NotificationAccountDeletionService,
) {

    /**
     * PUT /internal/notifications/reminder-schedules/{userId}
     * Called by user-service whenever a user's reminder settings change.
     */
    @PutMapping("/reminder-schedules/{userId}")
    fun upsertReminderSchedule(
        @PathVariable userId: UUID,
        @Valid @RequestBody request: UpsertReminderScheduleRequest,
    ): ResponseEntity<ReminderScheduleResponse> =
        ResponseEntity.ok(scheduleService.upsert(userId, request))

    /**
     * DELETE /internal/notifications/users/{userId}
     * Removes reminder schedules, notification logs, and registered FCM tokens for account deletion.
     */
    @DeleteMapping("/users/{userId}")
    fun deleteUserData(@PathVariable userId: UUID): ResponseEntity<Void> {
        accountDeletionService.deleteUserData(userId)
        return ResponseEntity.noContent().build()
    }

    /**
     * POST /internal/notifications/send
     * Sends a push notification to all active devices of the target user.
     * Used by other services (e.g. plan-service on plan generation).
     */
    @PostMapping("/send")
    fun send(
        @Valid @RequestBody request: SendNotificationRequest,
    ): ResponseEntity<SendNotificationResult> {
        val result = fcmService.sendToUser(
            userId = request.userId,
            payload = PushPayload(
                title = request.title,
                body = request.body,
                notificationType = request.notificationType,
                data = request.data,
            ),
        )
        return ResponseEntity.ok(SendNotificationResult(result.sent, result.failed, result.skipped))
    }
}
