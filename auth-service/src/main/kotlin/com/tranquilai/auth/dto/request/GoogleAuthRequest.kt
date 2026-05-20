package com.tranquilai.auth.dto.request

import jakarta.validation.constraints.NotBlank

data class GoogleAuthRequest(
    @field:NotBlank(message = "Google ID token is required")
    val idToken: String,
)

