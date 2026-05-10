package com.tranquilai.user.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.UUID

/** Called internally by auth-service after successful registration */
data class CreateUserRequest(
    val id: UUID,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val username: String,

    @field:NotBlank
    val firstName: String,

    @field:NotBlank
    val lastName: String,
)
