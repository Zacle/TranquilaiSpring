package com.tranquilai.notification.controller

import com.tranquilai.notification.dto.request.RegisterDeviceTokenRequest
import com.tranquilai.notification.dto.request.SendNotificationRequest
import com.tranquilai.notification.dto.request.UpsertReminderScheduleRequest
import com.tranquilai.notification.dto.response.DeviceTokenResponse
import com.tranquilai.notification.dto.response.NotificationLogResponse
import com.tranquilai.notification.dto.response.PageResponse
import com.tranquilai.notification.dto.response.ReminderScheduleResponse
import com.tranquilai.notification.security.GatewayUser
import com.tranquilai.notification.service.DeviceTokenService
import com.tranquilai.notification.service.FcmService
import com.tranquilai.notification.service.NotificationHistoryService
import com.tranquilai.notification.service.PushPayload
import com.tranquilai.notification.service.PushResult
import com.tranquilai.notification.service.ReminderScheduleService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import java.util.UUID

class NotificationControllersTest {

    private val user = GatewayUser(UUID.randomUUID(), "user@example.com", "USER")

    @Test
    fun `device token controller delegates register list and deactivate`() {
        val service: DeviceTokenService = mock(DeviceTokenService::class.java)
        val controller = DeviceTokenController(service)
        val request = RegisterDeviceTokenRequest("token", "Pixel")
        val response = tokenResponse(user.id)
        `when`(service.register(user.id, request)).thenReturn(response)
        `when`(service.listForUser(user.id)).thenReturn(listOf(response))

        assertEquals(HttpStatus.CREATED, controller.register(user, request).statusCode)
        assertEquals(listOf(response), controller.list(user).body)
        assertEquals(HttpStatus.NO_CONTENT, controller.deactivate(user, "token").statusCode)
        verify(service).deactivate(user.id, "token")
    }

    @Test
    fun `notification controller delegates history and returns no content when schedule missing`() {
        val history: NotificationHistoryService = mock(NotificationHistoryService::class.java)
        val schedules: ReminderScheduleService = mock(ReminderScheduleService::class.java)
        val controller = NotificationController(history, schedules)
        val page = PageResponse(listOf(logResponse(user.id)), 0, 20, 1, 1, true)
        `when`(history.getHistory(user.id, 0, 20)).thenReturn(page)
        `when`(schedules.get(user.id)).thenReturn(null)

        assertEquals(page, controller.history(user, 0, 20).body)
        assertEquals(HttpStatus.NO_CONTENT, controller.reminderSchedule(user).statusCode)
    }

    @Test
    fun `notification controller returns existing reminder schedule`() {
        val history: NotificationHistoryService = mock(NotificationHistoryService::class.java)
        val schedules: ReminderScheduleService = mock(ReminderScheduleService::class.java)
        val schedule = ReminderScheduleResponse(user.id, true, "DAILY", listOf("08:00"), 1)
        `when`(schedules.get(user.id)).thenReturn(schedule)

        assertEquals(schedule, NotificationController(history, schedules).reminderSchedule(user).body)
    }

    @Test
    fun `internal notification controller delegates schedule upsert and send`() {
        val fcm: FcmService = mock(FcmService::class.java)
        val schedules: ReminderScheduleService = mock(ReminderScheduleService::class.java)
        val controller = InternalNotificationController(fcm, schedules)
        val scheduleRequest = UpsertReminderScheduleRequest(true, "DAILY", listOf("08:00"))
        val scheduleResponse = ReminderScheduleResponse(user.id, true, "DAILY", listOf("08:00"), 1)
        val sendRequest = SendNotificationRequest(user.id, "Title", "Body", "CUSTOM", mapOf("k" to "v"))
        `when`(schedules.upsert(user.id, scheduleRequest)).thenReturn(scheduleResponse)
        `when`(fcm.sendToUser(user.id, PushPayload("Title", "Body", "CUSTOM", mapOf("k" to "v"))))
            .thenReturn(PushResult(1, 0, 0))

        assertEquals(scheduleResponse, controller.upsertReminderSchedule(user.id, scheduleRequest).body)
        assertEquals(1, controller.send(sendRequest).body?.sent)
    }

    private fun tokenResponse(userId: UUID) = DeviceTokenResponse(UUID.randomUUID(), userId, "Pixel", true, 1)

    private fun logResponse(userId: UUID) = NotificationLogResponse(
        id = UUID.randomUUID(),
        userId = userId,
        title = "Title",
        body = "Body",
        notificationType = "CUSTOM",
        status = "SENT",
        sentAt = 1,
    )
}
