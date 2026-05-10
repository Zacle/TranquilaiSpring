package com.tranquilai.ai.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)
class BadRequestException(message: String) : RuntimeException(message)
class PaymentRequiredException(message: String, val data: Map<String, Any?> = emptyMap()) : RuntimeException(message)
