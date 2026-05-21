package com.tranquilai.auth.event

import java.util.UUID

object AuthEventNames {
    const val VERIFICATION_EMAIL_REQUESTED = "auth.verification-email.requested"
    const val USER_VERIFIED = "auth.user.verified"
}

data class VerificationEmailRequestedEvent(
    val userId: UUID,
    val email: String,
    val firstName: String,
    val code: String,
)

data class UserVerifiedEvent(
    val userId: UUID,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
)
