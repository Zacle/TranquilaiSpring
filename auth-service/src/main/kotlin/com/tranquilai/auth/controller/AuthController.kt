package com.tranquilai.auth.controller

import com.tranquilai.auth.dto.request.*
import com.tranquilai.auth.dto.response.*
import com.tranquilai.auth.entity.User
import com.tranquilai.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.refreshToken(request))

    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal user: User): ResponseEntity<MessageResponse> {
        authService.logout(user.id)
        return ResponseEntity.ok(MessageResponse(message = "Logged out successfully"))
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<MessageResponse> =
        ResponseEntity.ok(authService.forgotPassword(request))

    @PostMapping("/verify-email")
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): ResponseEntity<EmailVerificationResponse> =
        ResponseEntity.ok(authService.verifyEmail(request))

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<MessageResponse> =
        ResponseEntity.ok(authService.resetPassword(request))

    @PostMapping("/resend-verification")
    fun resendVerification(@Valid @RequestBody request: ResendVerificationRequest): ResponseEntity<MessageResponse> =
        ResponseEntity.ok(authService.resendVerification(request))

    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<MessageResponse> =
        ResponseEntity.ok(authService.changePassword(user.id, request))
}
