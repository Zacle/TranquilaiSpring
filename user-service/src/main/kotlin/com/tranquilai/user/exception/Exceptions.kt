package com.tranquilai.user.exception

class UserNotFoundException(message: String) : RuntimeException(message)
class UserAlreadyExistsException(message: String) : RuntimeException(message)
class InvalidProfilePictureException(message: String) : RuntimeException(message)
class ProfilePictureUploadException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
