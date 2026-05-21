package com.tranquilai.user.event

import java.util.UUID

data class UserVerifiedEvent(
    val userId: UUID,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
)
