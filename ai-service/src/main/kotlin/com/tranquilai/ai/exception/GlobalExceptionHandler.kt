package com.tranquilai.ai.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ErrorResponse(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String,
    val errorCode: String? = null,
    val data: Map<String, Any>? = null,
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException) = ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse(status = 404, error = "NOT_FOUND", message = ex.message ?: "Resource not found", errorCode = "NOT_FOUND"))

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(ex: BadRequestException) = ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse(status = 400, error = "BAD_REQUEST", message = ex.message ?: "Bad request", errorCode = "BAD_REQUEST"))

    @ExceptionHandler(PaymentRequiredException::class)
    fun handlePaymentRequired(ex: PaymentRequiredException) = ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(
            ErrorResponse(
                status = 402,
                error = "PAYMENT_REQUIRED",
                message = ex.message ?: "Premium subscription required",
                errorCode = "PAYMENT_REQUIRED",
                data = ex.data.entries
                    .filter { it.value != null }
                    .associate { it.key to it.value as Any },
            ),
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException) = ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse(status = 400, error = "BAD_REQUEST", message = ex.message ?: "Invalid request", errorCode = "BAD_REQUEST"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(status = 400, error = "VALIDATION_FAILED", message = message, errorCode = "VALIDATION_FAILED"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception) = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse(status = 500, error = "INTERNAL_ERROR", message = "An unexpected error occurred", errorCode = "INTERNAL_ERROR"))
}
