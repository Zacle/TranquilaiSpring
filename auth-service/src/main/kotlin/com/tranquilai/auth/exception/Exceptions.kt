package com.tranquilai.auth.exception

class EmailAlreadyExistsException(message: String) : RuntimeException(message)
class UsernameAlreadyExistsException(message: String) : RuntimeException(message)
class InvalidCredentialsException(message: String) : RuntimeException(message)
class GoogleTokenVerificationException(message: String) : RuntimeException(message)
class EmailNotVerifiedException(message: String, val email: String? = null) : RuntimeException(message)
class EmailAlreadyVerifiedException(message: String) : RuntimeException(message)
class InvalidTokenException(message: String) : RuntimeException(message)
class InvalidVerificationCodeException(message: String) : RuntimeException(message)
class UserNotFoundException(message: String) : RuntimeException(message)
class AccountDeactivatedException(message: String) : RuntimeException(message)
