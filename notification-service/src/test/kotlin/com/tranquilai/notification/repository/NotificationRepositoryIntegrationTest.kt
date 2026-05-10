package com.tranquilai.notification.repository

import com.tranquilai.notification.entity.DeviceToken
import com.tranquilai.notification.entity.NotificationLog
import com.tranquilai.notification.entity.ReminderSchedule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
    ],
)
class NotificationRepositoryIntegrationTest @Autowired constructor(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val reminderScheduleRepository: ReminderScheduleRepository,
    private val notificationLogRepository: NotificationLogRepository,
) {

    @Test
    fun `device token repository finds active tokens by user ordered newest first`() {
        val userId = UUID.randomUUID()
        deviceTokenRepository.save(DeviceToken(userId = userId, token = "old", updatedAt = 1))
        deviceTokenRepository.save(DeviceToken(userId = userId, token = "new", updatedAt = 2))
        deviceTokenRepository.save(DeviceToken(userId = userId, token = "inactive", isActive = false, updatedAt = 3))

        assertEquals(listOf("new", "old"), deviceTokenRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId).map { it.token })
        assertTrue(deviceTokenRepository.findByToken("new").isPresent)
        assertTrue(deviceTokenRepository.existsByUserIdAndToken(userId, "new"))
    }

    @Test
    fun `reminder schedule repository finds enabled schedules by time and deletes by user`() {
        val userId = UUID.randomUUID()
        reminderScheduleRepository.save(ReminderSchedule(userId = userId, reminderTime = "08:00", enabled = true))
        reminderScheduleRepository.save(ReminderSchedule(userId = userId, reminderTime = "20:00", enabled = false))
        reminderScheduleRepository.save(ReminderSchedule(userId = UUID.randomUUID(), reminderTime = "08:00", enabled = true))

        assertEquals(2, reminderScheduleRepository.findEnabledByTime("08:00").size)
        assertEquals(2, reminderScheduleRepository.findAllByUserId(userId).size)

        reminderScheduleRepository.deleteAllByUserId(userId)
        reminderScheduleRepository.flush()
        assertTrue(reminderScheduleRepository.findAllByUserId(userId).isEmpty())
    }

    @Test
    fun `notification log repository returns user history ordered newest first`() {
        val userId = UUID.randomUUID()
        notificationLogRepository.save(NotificationLog(userId = userId, title = "old", body = "b", notificationType = "CUSTOM", status = "SENT", sentAt = 1))
        notificationLogRepository.save(NotificationLog(userId = userId, title = "new", body = "b", notificationType = "CUSTOM", status = "SENT", sentAt = 2))
        notificationLogRepository.save(NotificationLog(userId = UUID.randomUUID(), title = "other", body = "b", notificationType = "CUSTOM", status = "SENT", sentAt = 3))

        val page = notificationLogRepository.findByUserIdOrderBySentAtDesc(userId, PageRequest.of(0, 20))

        assertFalse(page.isEmpty)
        assertEquals(listOf("new", "old"), page.content.map { it.title })
    }
}
