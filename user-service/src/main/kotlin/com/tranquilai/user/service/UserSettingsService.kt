package com.tranquilai.user.service

import com.tranquilai.user.client.NotificationServiceClient
import com.tranquilai.user.dto.request.UpdateUserSettingsRequest
import com.tranquilai.user.dto.response.UserSettingsResponse
import com.tranquilai.user.entity.UserSettings
import com.tranquilai.user.exception.UserNotFoundException
import com.tranquilai.user.repository.UserRepository
import com.tranquilai.user.repository.UserSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class UserSettingsService(
    private val userRepository: UserRepository,
    private val settingsRepository: UserSettingsRepository,
    private val notificationClient: NotificationServiceClient,
) {
    @Transactional(readOnly = true)
    fun getSettings(userId: UUID): UserSettingsResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User $userId not found") }
        return getOrCreateSettings(user.id).toResponse()
    }

    fun updateSettings(userId: UUID, request: UpdateUserSettingsRequest): UserSettingsResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User $userId not found") }

        val settings = getOrCreateSettings(user.id)

        request.themePreference?.let { settings.themePreference = it }
        request.notificationsEnabled?.let { settings.notificationsEnabled = it }
        request.reminderEnabled?.let { settings.reminderEnabled = it }
        request.reminderTimes?.let { settings.reminderTimes = it.joinToString(",").ifEmpty { null } }
        request.reminderFrequency?.let { settings.reminderFrequency = it }
        request.preferredContentLanguage?.let { settings.preferredContentLanguage = it }
        request.showExplicitContent?.let { settings.showExplicitContent = it }
        settings.updatedAt = System.currentTimeMillis()

        val saved = settingsRepository.save(settings)

        // Sync reminder changes to notification-service (best-effort, non-blocking)
        val reminderTouched = request.reminderEnabled != null
                || request.reminderTimes != null
                || request.reminderFrequency != null
        if (reminderTouched) {
            val times = saved.reminderTimes?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            notificationClient.upsertReminderSchedule(
                userId = userId,
                enabled = saved.reminderEnabled,
                reminderTimes = times,
                frequency = saved.reminderFrequency,
            )
        }

        return saved.toResponse()
    }

    private fun getOrCreateSettings(userId: UUID): UserSettings {
        return settingsRepository.findByUserId(userId).orElseGet {
            val user = userRepository.findById(userId)
                .orElseThrow { UserNotFoundException("User $userId not found") }
            settingsRepository.save(UserSettings(user = user))
        }
    }
}

fun UserSettings.toResponse() = UserSettingsResponse(
    id = id,
    userId = user.id,
    themePreference = themePreference,
    notificationsEnabled = notificationsEnabled,
    reminderEnabled = reminderEnabled,
    reminderTimes = reminderTimes?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    reminderFrequency = reminderFrequency,
    preferredContentLanguage = preferredContentLanguage,
    showExplicitContent = showExplicitContent,
    updatedAt = updatedAt,
)
