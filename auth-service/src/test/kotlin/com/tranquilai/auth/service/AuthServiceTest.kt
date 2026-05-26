package com.tranquilai.auth.service

import com.tranquilai.auth.dto.request.ChangePasswordRequest
import com.tranquilai.auth.dto.request.ForgotPasswordRequest
import com.tranquilai.auth.dto.request.LoginRequest
import com.tranquilai.auth.dto.request.RefreshTokenRequest
import com.tranquilai.auth.dto.request.RegisterRequest
import com.tranquilai.auth.dto.request.ResetPasswordRequest
import com.tranquilai.auth.dto.request.VerifyEmailRequest
import com.tranquilai.auth.client.UserServiceClient
import com.tranquilai.auth.entity.RefreshToken
import com.tranquilai.auth.entity.User
import com.tranquilai.auth.exception.EmailAlreadyExistsException
import com.tranquilai.auth.exception.EmailNotVerifiedException
import com.tranquilai.auth.exception.InvalidCredentialsException
import com.tranquilai.auth.exception.InvalidTokenException
import com.tranquilai.auth.exception.InvalidVerificationCodeException
import com.tranquilai.auth.exception.UsernameAlreadyExistsException
import com.tranquilai.auth.repository.RefreshTokenRepository
import com.tranquilai.auth.repository.UserRepository
import com.tranquilai.auth.security.JwtService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val refreshTokenRepository: RefreshTokenRepository = mock(RefreshTokenRepository::class.java)
    private val passwordEncoder: PasswordEncoder = mock(PasswordEncoder::class.java)
    private val jwtService = JwtService(
        secret = "test-secret-that-is-long-enough-for-hs256!!",
        accessExpiration = 900_000,
    )
    private val emailService: EmailService = mock(EmailService::class.java)
    private val verificationCodeService: VerificationCodeService = mock(VerificationCodeService::class.java)
    private val outboxService: AuthOutboxService = mock(AuthOutboxService::class.java)
    private val userServiceClient: UserServiceClient = mock(UserServiceClient::class.java)

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            passwordEncoder = passwordEncoder,
            jwtService = jwtService,
            emailService = emailService,
            verificationCodeService = verificationCodeService,
            outboxService = outboxService,
            userServiceClient = userServiceClient,
            refreshExpiration = 604_800_000,
            googleWebClientId = "test-google-client-id",
        )

        `when`(refreshTokenRepository.save(any(RefreshToken::class.java)))
            .thenAnswer { invocation -> invocation.getArgument<RefreshToken>(0) }
    }

    @Test
    fun `register creates auth user queues verification email and token response`() {
        `when`(userRepository.existsByEmail("new@example.com")).thenReturn(false)
        `when`(userRepository.existsByUsername("new_user")).thenReturn(false)
        `when`(passwordEncoder.encode("password123")).thenReturn("encoded-password")
        `when`(userRepository.save(any(User::class.java))).thenAnswer { invocation -> invocation.getArgument<User>(0) }
        `when`(verificationCodeService.generateAndStoreEmailVerificationCode("new@example.com")).thenReturn("123456")

        val response = authService.register(
            RegisterRequest(
                email = "new@example.com",
                username = "new_user",
                password = "password123",
                firstName = "New",
                lastName = "User",
            ),
        )

        val userCaptor = ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).save(userCaptor.capture())
        val savedUser = userCaptor.value

        assertEquals("new@example.com", savedUser.email)
        assertEquals("new_user", savedUser.username)
        assertEquals("encoded-password", savedUser.passwordHash)
        assertFalse(savedUser.isEmailVerified)
        assertTrue(response.accessToken.isNotBlank())
        assertEquals(900, response.expiresIn)
        assertEquals("new@example.com", response.user.email)
        verify(outboxService).enqueueVerificationEmail(savedUser, "123456")
        verifyNoInteractions(emailService)
    }

    @Test
    fun `register rejects duplicate email`() {
        `when`(userRepository.existsByEmail("taken@example.com")).thenReturn(true)

        assertThrows(EmailAlreadyExistsException::class.java) {
            authService.register(registerRequest(email = "taken@example.com"))
        }

        verify(userRepository, never()).save(any(User::class.java))
        verifyNoInteractions(emailService)
    }

    @Test
    fun `register rejects duplicate explicit username`() {
        `when`(userRepository.existsByEmail("new@example.com")).thenReturn(false)
        `when`(userRepository.existsByUsername("taken")).thenReturn(true)

        assertThrows(UsernameAlreadyExistsException::class.java) {
            authService.register(registerRequest(email = "new@example.com", username = "taken"))
        }

        verify(userRepository, never()).save(any(User::class.java))
    }

    @Test
    fun `login returns tokens for verified active user`() {
        val user = user(email = "verified@example.com", passwordHash = "encoded", isEmailVerified = true)
        `when`(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches("password123", "encoded")).thenReturn(true)

        val response = authService.login(LoginRequest(email = "verified@example.com", password = "password123"))

        assertTrue(response.accessToken.isNotBlank())
        assertEquals("verified@example.com", response.user.email)
        verify(refreshTokenRepository).save(any(RefreshToken::class.java))
    }

    @Test
    fun `login rejects wrong password`() {
        val user = user(email = "verified@example.com", passwordHash = "encoded", isEmailVerified = true)
        `when`(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches("wrong", "encoded")).thenReturn(false)

        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(LoginRequest(email = "verified@example.com", password = "wrong"))
        }

        verify(refreshTokenRepository, never()).save(any(RefreshToken::class.java))
    }

    @Test
    fun `login rejects unverified email`() {
        val user = user(email = "pending@example.com", passwordHash = "encoded", isEmailVerified = false)
        `when`(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches("password123", "encoded")).thenReturn(true)

        assertThrows(EmailNotVerifiedException::class.java) {
            authService.login(LoginRequest(email = "pending@example.com", password = "password123"))
        }
    }

    @Test
    fun `refresh token revokes old token and issues new response`() {
        val user = user(isEmailVerified = true)
        val refreshToken = RefreshToken(
            user = user,
            token = "refresh-token",
            expiresAt = System.currentTimeMillis() + 60_000,
        )
        `when`(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken))

        val response = authService.refreshToken(RefreshTokenRequest("refresh-token"))

        assertTrue(refreshToken.isRevoked)
        assertTrue(response.accessToken.isNotBlank())
        verify(refreshTokenRepository).save(refreshToken)
    }

    @Test
    fun `refresh token rejects expired token`() {
        val expiredToken = RefreshToken(
            user = user(isEmailVerified = true),
            token = "expired",
            expiresAt = System.currentTimeMillis() - 60_000,
        )
        `when`(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(expiredToken))

        assertThrows(InvalidTokenException::class.java) {
            authService.refreshToken(RefreshTokenRequest("expired"))
        }
    }

    @Test
    fun `verify email marks user verified and deletes code`() {
        val user = user(email = "pending@example.com", isEmailVerified = false)
        `when`(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user))
        `when`(verificationCodeService.verifyEmailCode("pending@example.com", "123456")).thenReturn(true)
        `when`(userRepository.save(user)).thenReturn(user)

        val response = authService.verifyEmail(VerifyEmailRequest(email = "pending@example.com", code = "123456"))

        assertTrue(user.isEmailVerified)
        assertTrue(response.isVerified)
        verify(userRepository).save(user)
        verify(verificationCodeService).deleteEmailVerificationCode("pending@example.com")
        verify(userServiceClient).createUser(user)
        verify(outboxService).enqueueUserVerified(user)
    }

    @Test
    fun `verify email rejects invalid code`() {
        val user = user(email = "pending@example.com", isEmailVerified = false)
        `when`(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user))
        `when`(verificationCodeService.verifyEmailCode("pending@example.com", "000000")).thenReturn(false)

        assertThrows(InvalidVerificationCodeException::class.java) {
            authService.verifyEmail(VerifyEmailRequest(email = "pending@example.com", code = "000000"))
        }

        assertFalse(user.isEmailVerified)
        verify(userRepository, never()).save(user)
    }

    @Test
    fun `forgot password is intentionally generic for unknown email`() {
        `when`(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty())

        val response = authService.forgotPassword(ForgotPasswordRequest("missing@example.com"))

        assertEquals("If the email exists, a reset code will be sent", response.message)
        assertEquals("missing@example.com", response.email)
        verifyNoInteractions(emailService)
    }

    @Test
    fun `reset password updates password revokes tokens and deletes code`() {
        val user = user(email = "verified@example.com", passwordHash = "old", isEmailVerified = true)
        `when`(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user))
        `when`(verificationCodeService.verifyPasswordResetCode("verified@example.com", "123456")).thenReturn(true)
        `when`(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded")
        `when`(userRepository.save(user)).thenReturn(user)

        val response = authService.resetPassword(
            ResetPasswordRequest(
                email = "verified@example.com",
                code = "123456",
                newPassword = "newPassword123",
            ),
        )

        assertEquals("Password reset successfully", response.message)
        assertEquals("new-encoded", user.passwordHash)
        verify(refreshTokenRepository).revokeAllByUser(user)
        verify(verificationCodeService).deletePasswordResetCode("verified@example.com")
    }

    @Test
    fun `change password requires current password`() {
        val userId = UUID.randomUUID()
        val user = user(id = userId, passwordHash = "old", isEmailVerified = true)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches("wrong", "old")).thenReturn(false)

        assertThrows(InvalidCredentialsException::class.java) {
            authService.changePassword(
                userId,
                ChangePasswordRequest(currentPassword = "wrong", newPassword = "newPassword123"),
            )
        }

        verify(userRepository, never()).save(user)
        verify(refreshTokenRepository, never()).revokeAllByUser(user)
    }

    @Test
    fun `change password updates hash and revokes refresh tokens`() {
        val userId = UUID.randomUUID()
        val user = user(id = userId, passwordHash = "old", isEmailVerified = true)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches("currentPassword", "old")).thenReturn(true)
        `when`(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded")
        `when`(userRepository.save(user)).thenReturn(user)

        val response = authService.changePassword(
            userId,
            ChangePasswordRequest(currentPassword = "currentPassword", newPassword = "newPassword123"),
        )

        assertEquals("Password changed successfully", response.message)
        assertEquals("new-encoded", user.passwordHash)
        verify(refreshTokenRepository).revokeAllByUser(user)
    }

    private fun registerRequest(
        email: String = "new@example.com",
        username: String? = "new_user",
    ) = RegisterRequest(
        email = email,
        username = username,
        password = "password123",
        firstName = "New",
        lastName = "User",
    )

    private fun user(
        id: UUID = UUID.randomUUID(),
        email: String = "user@example.com",
        username: String = "user",
        passwordHash: String = "encoded-password",
        isEmailVerified: Boolean = false,
        isActive: Boolean = true,
    ) = User(
        id = id,
        email = email,
        username = username,
        passwordHash = passwordHash,
        firstName = "Test",
        lastName = "User",
        isEmailVerified = isEmailVerified,
        isActive = isActive,
    )
}
