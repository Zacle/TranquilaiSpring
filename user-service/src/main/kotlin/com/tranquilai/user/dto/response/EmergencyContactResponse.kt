package com.tranquilai.user.dto.response

import java.util.UUID

data class EmergencyContactResponse(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val phoneNumber: String?,
    val email: String?,
    val relationship: String,
    val isPrimary: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
