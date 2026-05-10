package com.tranquilai.user.dto.request

import jakarta.validation.constraints.NotBlank

data class CreateEmergencyContactRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    val phoneNumber: String? = null,
    val email: String? = null,

    @field:NotBlank(message = "Relationship is required")
    val relationship: String,

    val isPrimary: Boolean = false,
)

data class UpdateEmergencyContactRequest(
    val name: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val relationship: String? = null,
)
