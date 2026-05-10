package com.tranquilai.auth.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
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
            ErrorResponse(
                status = 400,
                error = "VALIDATION_FAILED",
                errorCode = "VALIDATION_FAILED",
                message = "Request validation failed",
                details = errors,
            )
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException) =
        ResponseEntity.badRequest().body(
            ErrorResponse(status = 400, error = "MALFORMED_REQUEST", errorCode = "MALFORMED_REQUEST", message = "Request body is missing or malformed")
        )

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(ex: HttpRequestMethodNotSupportedException) =
        ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
            ErrorResponse(status = 405, error = "METHOD_NOT_ALLOWED", errorCode = "METHOD_NOT_ALLOWED", message = "HTTP method '${ex.method}' is not supported for this endpoint")
        )

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailExists(ex: EmailAlreadyExistsException) =
        conflict(ex.message!!, "EMAIL_ALREADY_EXISTS")

    @ExceptionHandler(UsernameAlreadyExistsException::class)
    fun handleUsernameExists(ex: UsernameAlreadyExistsException) =
        conflict(ex.message!!, "USERNAME_ALREADY_EXISTS")

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException) =
        unauthorized(ex.message!!, "INVALID_CREDENTIALS")

    @ExceptionHandler(EmailNotVerifiedException::class)
    fun handleEmailNotVerified(ex: EmailNotVerifiedException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(
                status = 403,
                error = "EMAIL_NOT_VERIFIED",
                errorCode = "EMAIL_NOT_VERIFIED",
                message = ex.message!!,
                data = ex.email?.let { mapOf("email" to it) },
            )
        )

    @ExceptionHandler(EmailAlreadyVerifiedException::class)
    fun handleEmailAlreadyVerified(ex: EmailAlreadyVerifiedException) =
        conflict(ex.message!!, "EMAIL_ALREADY_VERIFIED")

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(ex: InvalidTokenException) =
        unauthorized(ex.message!!, "INVALID_TOKEN")

    @ExceptionHandler(InvalidVerificationCodeException::class)
    fun handleInvalidCode(ex: InvalidVerificationCodeException) =
        ResponseEntity.badRequest().body(
            ErrorResponse(status = 400, error = "INVALID_CODE", errorCode = "INVALID_CODE", message = ex.message!!)
        )

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(status = 404, error = "USER_NOT_FOUND", errorCode = "USER_NOT_FOUND", message = ex.message!!)
        )

    @ExceptionHandler(AccountDeactivatedException::class)
    fun handleAccountDeactivated(ex: AccountDeactivatedException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(status = 403, error = "ACCOUNT_DEACTIVATED", errorCode = "ACCOUNT_DEACTIVATED", message = ex.message!!)
        )

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(status = 500, error = "INTERNAL_ERROR", errorCode = "INTERNAL_ERROR", message = "An unexpected error occurred")
        )
    }

    private fun conflict(message: String, code: String) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(status = 409, error = code, errorCode = code, message = message)
        )

    private fun unauthorized(message: String, code: String) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(status = 401, error = code, errorCode = code, message = message)
        )
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
