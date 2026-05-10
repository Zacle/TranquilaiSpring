package com.tranquilai.auth.dto.response

import com.tranquilai.auth.entity.OnboardingStatus
import com.tranquilai.auth.entity.UserRole
import java.util.UUID

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse,
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val onboardingStatus: OnboardingStatus,
    val profilePictureUrl: String?,
    val isEmailVerified: Boolean,
    val roles: Set<UserRole>,
    val languagePreference: String,
    val createdAt: Long,
)

data class MessageResponse(
    val message: String,
    val email: String? = null,
)

data class EmailVerificationResponse(
    val message: String,
    val email: String,
    val isVerified: Boolean,
)
