package com.tranquilai.notification.dto.request

import jakarta.validation.constraints.NotBlank

data class RegisterDeviceTokenRequest(
    @field:NotBlank val token: String,
    val deviceName: String? = null,
)
