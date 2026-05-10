package com.tranquilai.user.service

import com.tranquilai.user.client.NotificationServiceClient
import com.tranquilai.user.dto.request.UpdateUserSettingsRequest
import com.tranquilai.user.entity.User
import com.tranquilai.user.entity.UserSettings
import com.tranquilai.user.exception.UserNotFoundException
import com.tranquilai.user.repository.UserRepository
import com.tranquilai.user.repository.UserSettingsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class UserSettingsServiceTest {

    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val settingsRepository: UserSettingsRepository = mock(UserSettingsRepository::class.java)
    private val notificationClient: NotificationServiceClient = mock(NotificationServiceClient::class.java)
    private val service = UserSettingsService(userRepository, settingsRepository, notificationClient)

    @Test
    fun `getSettings creates defaults when settings are missing`() {
        val user = testUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(settingsRepository.findByUserId(user.id)).thenReturn(Optional.empty())
        `when`(settingsRepository.save(anySettings())).thenAnswer { it.getArgument<UserSettings>(0) }

        val response = service.getSettings(user.id)

        assertEquals(user.id, response.userId)
        assertEquals("SYSTEM", response.themePreference)
        assertTrue(response.notificationsEnabled)
        assertFalse(response.reminderEnabled)
        verify(settingsRepository).save(anySettings())
    }

    @Test
    fun `updateSettings saves preferences and syncs reminder changes`() {
        val user = testUser()
        val settings = UserSettings(user = user)
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(settingsRepository.findByUserId(user.id)).thenReturn(Optional.of(settings))
        `when`(settingsRepository.save(settings)).thenReturn(settings)

        val response = service.updateSettings(
            user.id,
            UpdateUserSettingsRequest(
                themePreference = "DARK",
                notificationsEnabled = false,
                reminderEnabled = true,
                reminderTimes = listOf("08:00", "20:00"),
                reminderFrequency = "WEEKDAYS",
                preferredContentLanguage = "ar",
                showExplicitContent = true,
            ),
        )

        assertEquals("DARK", response.themePreference)
        assertFalse(response.notificationsEnabled)
        assertTrue(response.reminderEnabled)
        assertEquals(listOf("08:00", "20:00"), response.reminderTimes)
        assertEquals("WEEKDAYS", response.reminderFrequency)
        assertEquals("ar", response.preferredContentLanguage)
        assertTrue(response.showExplicitContent)
        verify(notificationClient).upsertReminderSchedule(user.id, true, listOf("08:00", "20:00"), "WEEKDAYS")
    }

    @Test
    fun `updateSettings does not sync notifications when reminder fields are untouched`() {
        val user = testUser()
        val settings = UserSettings(user = user)
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(settingsRepository.findByUserId(user.id)).thenReturn(Optional.of(settings))
        `when`(settingsRepository.save(settings)).thenReturn(settings)

        service.updateSettings(user.id, UpdateUserSettingsRequest(themePreference = "LIGHT"))

        verifyNoInteractions(notificationClient)
    }

    @Test
    fun `getSettings throws when user is missing`() {
        val userId = UUID.randomUUID()
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows(UserNotFoundException::class.java) {
            service.getSettings(userId)
        }
    }

    private fun testUser() = User(
        id = UUID.randomUUID(),
        email = "user@example.com",
        username = "user",
        firstName = "Test",
        lastName = "User",
    )

    @Suppress("UNCHECKED_CAST")
    private fun anySettings(): UserSettings {
        any(UserSettings::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
