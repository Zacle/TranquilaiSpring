package com.tranquilai.auth.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class VerifyEmailRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Verification code is required")
    @field:Size(min = 6, max = 6, message = "Verification code must be 6 digits")
    val code: String,
)
