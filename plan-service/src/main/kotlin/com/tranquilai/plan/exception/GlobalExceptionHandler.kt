package com.tranquilai.plan.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

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
    val data: Map<String, Any>? = null,
)
