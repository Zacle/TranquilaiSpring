package com.tranquilai.notification.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UpsertReminderScheduleRequest(
    val enabled: Boolean,
    /** DAILY | WEEKDAYS | WEEKENDS */
    @field:NotBlank val frequency: String = "DAILY",
    /** List of HH:mm reminder times — replaces all existing schedules for the user */
    val reminderTimes: List<
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "each reminderTime must be HH:mm")
        String
    > = emptyList(),
)
