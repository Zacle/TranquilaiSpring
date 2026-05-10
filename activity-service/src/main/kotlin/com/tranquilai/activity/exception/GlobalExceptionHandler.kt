package com.tranquilai.activity.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.associate { error ->
            val field = (error as? FieldError)?.field ?: "global"
            field to (error.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity.badRequest().body(
            ErrorResponse(status = 400, error = "VALIDATION_FAILED", message = "Request validation failed", errorCode = "VALIDATION_FAILED", details = errors)
        )
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(status = 404, error = "NOT_FOUND", message = ex.message!!, errorCode = "NOT_FOUND")
        )

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
    val details: Map<String, String>? = null,
    val data: Map<String, Any>? = null,
)
