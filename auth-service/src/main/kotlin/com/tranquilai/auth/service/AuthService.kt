package com.tranquilai.auth.service

import com.tranquilai.auth.client.UserServiceClient
import com.tranquilai.auth.dto.request.*
import com.tranquilai.auth.dto.response.*
import com.tranquilai.auth.entity.RefreshToken
import com.tranquilai.auth.entity.User
import com.tranquilai.auth.exception.*
import com.tranquilai.auth.repository.RefreshTokenRepository
import com.tranquilai.auth.repository.UserRepository
import com.tranquilai.auth.security.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailService: EmailService,
    private val verificationCodeService: VerificationCodeService,
    private val userServiceClient: UserServiceClient,
    @param:Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
) {
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException("Email '${request.email}' is already registered")
        }

        val username = request.username ?: generateUsernameFromEmail(request.email)
        if (userRepository.existsByUsername(username)) {
            throw UsernameAlreadyExistsException("Username '$username' is already taken")
        }

        val user = User(
            email = request.email,
            username = username,
            passwordHash = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
        )
        userRepository.save(user)

        // Sync user profile to user-service (fire-and-forget)
        userServiceClient.createUser(user)

        // Send verification email
        val code = verificationCodeService.generateAndStoreEmailVerificationCode(user.email)
        emailService.sendVerificationEmail(user.email, user.firstName, code)

        return buildAuthResponse(user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { InvalidCredentialsException("Invalid email or password") }

        if (!user.isActive) {
            throw AccountDeactivatedException("Account is deactivated")
        }
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid email or password")
        }
        if (!user.isEmailVerified) {
            throw EmailNotVerifiedException("Please verify your email before logging in", email = user.email)
        }

        return buildAuthResponse(user)
    }

    fun refreshToken(request: RefreshTokenRequest): AuthResponse {
        val refreshToken = refreshTokenRepository.findByToken(request.refreshToken)
            .orElseThrow { InvalidTokenException("Refresh token not found") }

        if (!refreshToken.isValid()) {
            throw InvalidTokenException("Refresh token is expired or revoked")
        }

        refreshToken.isRevoked = true
        refreshTokenRepository.save(refreshToken)

        return buildAuthResponse(refreshToken.user)
    }

    fun logout(userId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        refreshTokenRepository.revokeAllByUser(user)
    }

    fun forgotPassword(request: ForgotPasswordRequest): MessageResponse {
        val user = userRepository.findByEmail(request.email)
            .orElse(null) ?: return MessageResponse(
            message = "If the email exists, a reset code will be sent",
            email = request.email,
        )

        val code = verificationCodeService.generateAndStorePasswordResetCode(user.email)
        emailService.sendPasswordResetEmail(user.email, user.firstName, code)

        return MessageResponse(
            message = "Password reset code sent to your email",
            email = user.email,
        )
    }

    fun verifyEmail(request: VerifyEmailRequest): EmailVerificationResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { UserNotFoundException("User not found") }

        if (user.isEmailVerified) {
            return EmailVerificationResponse(
                message = "Email is already verified",
                email = user.email,
                isVerified = true,
            )
        }

        if (!verificationCodeService.verifyEmailCode(request.email, request.code)) {
            throw InvalidVerificationCodeException("Invalid or expired verification code")
        }

        user.isEmailVerified = true
        user.updatedAt = System.currentTimeMillis()
        userRepository.save(user)

        verificationCodeService.deleteEmailVerificationCode(request.email)

        return EmailVerificationResponse(
            message = "Email verified successfully",
            email = user.email,
            isVerified = true,
        )
    }

    fun resetPassword(request: ResetPasswordRequest): MessageResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { UserNotFoundException("User not found") }

        if (!verificationCodeService.verifyPasswordResetCode(request.email, request.code)) {
            throw InvalidVerificationCodeException("Invalid or expired reset code")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        user.updatedAt = System.currentTimeMillis()
        userRepository.save(user)

        // Revoke all refresh tokens after password reset
        refreshTokenRepository.revokeAllByUser(user)
        verificationCodeService.deletePasswordResetCode(request.email)

        return MessageResponse(message = "Password reset successfully")
    }

    fun resendVerification(request: ResendVerificationRequest): MessageResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { UserNotFoundException("User not found") }

        if (user.isEmailVerified) {
            throw EmailAlreadyVerifiedException("Email is already verified")
        }

        val code = verificationCodeService.generateAndStoreEmailVerificationCode(user.email)
        emailService.sendVerificationEmail(user.email, user.firstName, code)

        return MessageResponse(
            message = "Verification email resent",
            email = user.email,
        )
    }

    fun deactivateUser(userId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        user.isActive = false
        user.updatedAt = System.currentTimeMillis()
        userRepository.save(user)
        refreshTokenRepository.revokeAllByUser(user)
    }

    fun deleteUser(userId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        refreshTokenRepository.revokeAllByUser(user)
        userRepository.delete(user)
    }

    fun changePassword(userId: UUID, request: ChangePasswordRequest): MessageResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw InvalidCredentialsException("Current password is incorrect")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        user.updatedAt = System.currentTimeMillis()
        userRepository.save(user)

        refreshTokenRepository.revokeAllByUser(user)

        return MessageResponse(message = "Password changed successfully")
    }

    private fun generateUsernameFromEmail(email: String): String {
        val base = email.substringBefore("@").replace(Regex("[^a-zA-Z0-9_]"), "").take(40)
        if (!userRepository.existsByUsername(base)) return base
        // Append random suffix if base username is taken
        repeat(10) {
            val candidate = "${base}_${(1000..9999).random()}"
            if (!userRepository.existsByUsername(candidate)) return candidate
        }
        return "${base}_${UUID.randomUUID().toString().take(8)}"
    }

    private fun buildAuthResponse(user: User): AuthResponse {
        val accessToken = jwtService.generateAccessToken(user.id, user.email, user.roles)

        val refreshTokenEntity = RefreshToken(
            user = user,
            token = UUID.randomUUID().toString() + "." + UUID.randomUUID().toString(),
            expiresAt = System.currentTimeMillis() + refreshExpiration,
        )
        refreshTokenRepository.save(refreshTokenEntity)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenEntity.token,
            expiresIn = jwtService.getAccessExpirationMs() / 1000,
            user = user.toResponse(),
        )
    }
}

private fun User.toResponse() = UserResponse(
    id = id,
    email = email,
    username = username,
    firstName = firstName,
    lastName = lastName,
    onboardingStatus = onboardingStatus,
    profilePictureUrl = profilePictureUrl,
    isEmailVerified = isEmailVerified,
    roles = getRoleSet(),
    languagePreference = languagePreference,
    createdAt = createdAt,
)
