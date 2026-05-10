package com.tranquilai.user.dto.request

data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: Long? = null,
    val phoneNumber: String? = null,
    val timezone: String? = null,
    val languagePreference: String? = null,
    val profilePictureUrl: String? = null,
)
