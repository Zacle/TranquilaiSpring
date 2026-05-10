package com.tranquilai.user.exception

class UserNotFoundException(message: String) : RuntimeException(message)
class UserAlreadyExistsException(message: String) : RuntimeException(message)
