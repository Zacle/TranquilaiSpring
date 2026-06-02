package com.tranquilai.user.exception

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

    @ExceptionHandler(UserNotFoundException::class)
    fun handleNotFound(ex: UserNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(status = 404, error = "USER_NOT_FOUND", message = ex.message!!, errorCode = "USER_NOT_FOUND")
        )

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleConflict(ex: UserAlreadyExistsException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(status = 409, error = "USER_ALREADY_EXISTS", message = ex.message!!, errorCode = "USER_ALREADY_EXISTS")
        )

    @ExceptionHandler(InvalidProfilePictureException::class)
    fun handleInvalidProfilePicture(ex: InvalidProfilePictureException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(status = 400, error = "INVALID_PROFILE_PICTURE", message = ex.message!!, errorCode = "INVALID_PROFILE_PICTURE")
        )

    @ExceptionHandler(ProfilePictureUploadException::class)
    fun handleProfilePictureUpload(ex: ProfilePictureUploadException): ResponseEntity<ErrorResponse> {
        logger.error("Profile picture upload failed: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            ErrorResponse(status = 502, error = "PROFILE_PICTURE_UPLOAD_FAILED", message = ex.message!!, errorCode = "PROFILE_PICTURE_UPLOAD_FAILED")
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
    val details: Map<String, String>? = null,
    val data: Map<String, Any>? = null,
)
