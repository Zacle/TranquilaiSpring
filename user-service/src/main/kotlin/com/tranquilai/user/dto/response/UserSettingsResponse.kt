package com.tranquilai.user.dto.response

import java.util.UUID

data class UserSettingsResponse(
    val id: UUID,
    val userId: UUID,
    val themePreference: String,
    val notificationsEnabled: Boolean,
    val reminderEnabled: Boolean,
    val reminderTimes: List<String>,
    val reminderFrequency: String,
    val preferredContentLanguage: String,
    val showExplicitContent: Boolean,
    val updatedAt: Long,
)
