package com.tranquilai.user.dto.response

import com.tranquilai.user.entity.OnboardingStatus
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val dateOfBirth: Long?,
    val phoneNumber: String?,
    val timezone: String?,
    val languagePreference: String,
    val onboardingStatus: OnboardingStatus,
    val profilePictureUrl: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
