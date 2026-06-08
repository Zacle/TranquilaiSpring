package com.tranquilai.notification.service

import com.tranquilai.notification.repository.DeviceTokenRepository
import com.tranquilai.notification.repository.NotificationLogRepository
import com.tranquilai.notification.repository.ReminderScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationAccountDeletionService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val reminderScheduleRepository: ReminderScheduleRepository,
    private val notificationLogRepository: NotificationLogRepository,
) {
    @Transactional
    fun deleteUserData(userId: UUID) {
        reminderScheduleRepository.deleteAllByUserId(userId)
        notificationLogRepository.deleteByUserId(userId)
        deviceTokenRepository.deleteByUserId(userId)
    }
}
