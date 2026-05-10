package com.tranquilai.notification.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(status = 404, error = "NOT_FOUND", message = ex.message ?: "Resource not found", errorCode = "NOT_FOUND")
        )

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalid(ex: InvalidRequestException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(status = 400, error = "BAD_REQUEST", message = ex.message ?: "Invalid request", errorCode = "BAD_REQUEST")
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString(", ") {
            "${it.field}: ${it.defaultMessage}"
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(status = 400, error = "VALIDATION_ERROR", message = message, errorCode = "VALIDATION_ERROR")
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(status = 500, error = "INTERNAL_ERROR", message = "An unexpected error occurred", errorCode = "INTERNAL_ERROR")
        )
    }
}

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val errorCode: String? = null,
    val timestamp: Long = Instant.now().toEpochMilli(),
    val data: Map<String, Any>? = null,
)
