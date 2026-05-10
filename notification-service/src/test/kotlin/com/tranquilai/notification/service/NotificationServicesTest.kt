package com.tranquilai.notification.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.tranquilai.notification.dto.request.RegisterDeviceTokenRequest
import com.tranquilai.notification.dto.request.UpsertReminderScheduleRequest
import com.tranquilai.notification.entity.DeviceToken
import com.tranquilai.notification.entity.NotificationLog
import com.tranquilai.notification.entity.ReminderSchedule
import com.tranquilai.notification.exception.ResourceNotFoundException
import com.tranquilai.notification.repository.DeviceTokenRepository
import com.tranquilai.notification.repository.NotificationLogRepository
import com.tranquilai.notification.repository.ReminderScheduleRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import java.util.UUID

class NotificationServicesTest {

    @Test
    fun `device token register creates token when missing`() {
        val userId = UUID.randomUUID()
        val repo: DeviceTokenRepository = mock(DeviceTokenRepository::class.java)
        `when`(repo.findByToken("token-1")).thenReturn(Optional.empty())
        `when`(repo.save(anyDeviceToken())).thenAnswer { it.getArgument<DeviceToken>(0) }

        val response = DeviceTokenService(repo).register(userId, RegisterDeviceTokenRequest("token-1", "Pixel"))

        assertEquals(userId, response.userId)
        assertEquals("Pixel", response.deviceName)
        assertTrue(response.isActive)
    }

    @Test
    fun `device token register refreshes existing token and deactivate validates owner`() {
        val userId = UUID.randomUUID()
        val token = DeviceToken(userId = userId, token = "token-1", deviceName = "Old", isActive = false)
        val repo: DeviceTokenRepository = mock(DeviceTokenRepository::class.java)
        `when`(repo.findByToken("token-1")).thenReturn(Optional.of(token))
        `when`(repo.save(token)).thenReturn(token)
        val service = DeviceTokenService(repo)

        val response = service.register(userId, RegisterDeviceTokenRequest("token-1", "New"))
        assertTrue(response.isActive)
        assertEquals("New", response.deviceName)

        service.deactivate(userId, "token-1")
        assertFalse(token.isActive)

        val otherToken = DeviceToken(userId = UUID.randomUUID(), token = "other")
        `when`(repo.findByToken("other")).thenReturn(Optional.of(otherToken))
        assertThrows(ResourceNotFoundException::class.java) { service.deactivate(userId, "other") }
    }

    @Test
    fun `device token list returns active tokens in repository order`() {
        val userId = UUID.randomUUID()
        val repo: DeviceTokenRepository = mock(DeviceTokenRepository::class.java)
        `when`(repo.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)).thenReturn(
            listOf(DeviceToken(userId = userId, token = "new", updatedAt = 2), DeviceToken(userId = userId, token = "old", updatedAt = 1)),
        )

        assertEquals(2, DeviceTokenService(repo).listForUser(userId).size)
    }

    @Test
    fun `reminder schedule upsert replaces schedules and returns sorted times`() {
        val userId = UUID.randomUUID()
        val repo: ReminderScheduleRepository = mock(ReminderScheduleRepository::class.java)
        val request = UpsertReminderScheduleRequest(enabled = true, frequency = "WEEKDAYS", reminderTimes = listOf("20:00", "08:00"))

        val response = ReminderScheduleService(repo).upsert(userId, request)

        verify(repo).deleteAllByUserId(userId)
        verify(repo).saveAll(anyScheduleList())
        assertEquals(listOf("08:00", "20:00"), response.reminderTimes)
        assertEquals("WEEKDAYS", response.frequency)
    }

    @Test
    fun `reminder schedule get returns latest settings and sorted times`() {
        val userId = UUID.randomUUID()
        val repo: ReminderScheduleRepository = mock(ReminderScheduleRepository::class.java)
        `when`(repo.findAllByUserId(userId)).thenReturn(
            listOf(
                ReminderSchedule(userId = userId, reminderTime = "20:00", frequency = "DAILY", enabled = true, updatedAt = 10),
                ReminderSchedule(userId = userId, reminderTime = "08:00", frequency = "WEEKENDS", enabled = false, updatedAt = 20),
            ),
        )

        val response = ReminderScheduleService(repo).get(userId)

        assertEquals(false, response?.enabled)
        assertEquals("WEEKENDS", response?.frequency)
        assertEquals(listOf("08:00", "20:00"), response?.reminderTimes)
    }

    @Test
    fun `notification history maps page response`() {
        val userId = UUID.randomUUID()
        val repo: NotificationLogRepository = mock(NotificationLogRepository::class.java)
        val log = NotificationLog(userId = userId, title = "Title", body = "Body", notificationType = "CUSTOM", status = "SENT")
        `when`(repo.findByUserIdOrderBySentAtDesc(userId, PageRequest.of(0, 20))).thenReturn(PageImpl(listOf(log), PageRequest.of(0, 20), 1))

        val response = NotificationHistoryService(repo).getHistory(userId, 0, 20)

        assertEquals(1, response.totalElements)
        assertEquals("Title", response.content.single().title)
    }

    @Test
    fun `fcm send skips users without active tokens`() {
        val userId = UUID.randomUUID()
        val messaging: FirebaseMessaging = mock(FirebaseMessaging::class.java)
        val tokenRepo: DeviceTokenRepository = mock(DeviceTokenRepository::class.java)
        val logRepo: NotificationLogRepository = mock(NotificationLogRepository::class.java)
        `when`(tokenRepo.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)).thenReturn(emptyList())

        val result = FcmService(messaging, tokenRepo, logRepo).sendToUser(userId, PushPayload("T", "B", "CUSTOM"))

        assertEquals(PushResult(sent = 0, failed = 0, skipped = 1), result)
        verify(logRepo, never()).save(anyNotificationLog())
    }

    @Test
    fun `fcm send logs successful sends for each active token`() {
        val userId = UUID.randomUUID()
        val messaging: FirebaseMessaging = mock(FirebaseMessaging::class.java)
        val tokenRepo: DeviceTokenRepository = mock(DeviceTokenRepository::class.java)
        val logRepo: NotificationLogRepository = mock(NotificationLogRepository::class.java)
        `when`(tokenRepo.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)).thenReturn(
            listOf(DeviceToken(userId = userId, token = "token-1"), DeviceToken(userId = userId, token = "token-2")),
        )
        `when`(messaging.send(anyMessage())).thenReturn("message-id")
        `when`(logRepo.save(anyNotificationLog())).thenAnswer { it.getArgument<NotificationLog>(0) }

        val result = FcmService(messaging, tokenRepo, logRepo).sendToUser(userId, PushPayload("T", "B", "CUSTOM", mapOf("screen" to "home")))

        assertEquals(PushResult(sent = 2, failed = 0, skipped = 0), result)
        verify(logRepo, org.mockito.Mockito.times(2)).save(anyNotificationLog())
    }

    private fun anyDeviceToken(): DeviceToken {
        any(DeviceToken::class.java)
        return uninitialized()
    }

    private fun anyNotificationLog(): NotificationLog {
        any(NotificationLog::class.java)
        return uninitialized()
    }

    private fun anyMessage(): Message {
        any(Message::class.java)
        return uninitialized()
    }

    private fun anyScheduleList(): List<ReminderSchedule> {
        any(List::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
