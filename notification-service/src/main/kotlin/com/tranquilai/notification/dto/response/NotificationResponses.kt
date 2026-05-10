package com.tranquilai.notification.dto.response

import java.util.UUID

data class DeviceTokenResponse(
    val id: UUID,
    val userId: UUID,
    val deviceName: String?,
    val isActive: Boolean,
    val createdAt: Long,
)

data class ReminderScheduleResponse(
    val userId: UUID,
    val enabled: Boolean,
    val frequency: String,
    /** All scheduled reminder times for this user in HH:mm format */
    val reminderTimes: List<String>,
    val updatedAt: Long,
)

data class NotificationLogResponse(
    val id: UUID,
    val userId: UUID,
    val title: String,
    val body: String,
    val notificationType: String,
    val status: String,
    val sentAt: Long,
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
)

data class SendNotificationResult(
    val sent: Int,
    val failed: Int,
    val skipped: Int,
)
