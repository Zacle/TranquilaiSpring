package com.tranquilai.notification.dto.request

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class SendNotificationRequest(
    val userId: UUID,
    @field:NotBlank val title: String,
    @field:NotBlank val body: String,
    val notificationType: String = "CUSTOM",
    val data: Map<String, String> = emptyMap(),
)
