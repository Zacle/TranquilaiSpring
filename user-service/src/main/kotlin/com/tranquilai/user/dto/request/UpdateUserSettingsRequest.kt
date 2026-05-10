package com.tranquilai.user.dto.request

data class UpdateUserSettingsRequest(
    val themePreference: String? = null,
    val notificationsEnabled: Boolean? = null,
    val reminderEnabled: Boolean? = null,
    val reminderTimes: List<String>? = null,
    val reminderFrequency: String? = null,
    val preferredContentLanguage: String? = null,
    val showExplicitContent: Boolean? = null,
)
