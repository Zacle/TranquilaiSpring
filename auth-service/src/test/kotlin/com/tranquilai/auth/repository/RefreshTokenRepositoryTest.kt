package com.tranquilai.auth.repository

import com.tranquilai.auth.entity.RefreshToken
import com.tranquilai.auth.entity.User
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    ],
)
class RefreshTokenRepositoryTest @Autowired constructor(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `findByToken returns matching refresh token`() {
        val user = insertUser(email = "find@example.com", username = "find")
        val refreshToken = insertRefreshToken(
            RefreshToken(
                user = user,
                token = "find-token",
                expiresAt = System.currentTimeMillis() + 60_000,
            ),
        )

        val found = refreshTokenRepository.findByToken("find-token")

        assertTrue(found.isPresent)
        assertEquals(refreshToken.id, found.get().id)
    }

    @Test
    fun `revokeAllByUser revokes only tokens for the requested user`() {
        val firstUser = insertUser(email = "first@example.com", username = "first")
        val secondUser = insertUser(email = "second@example.com", username = "second")
        insertRefreshToken(RefreshToken(user = firstUser, token = "first-1", expiresAt = future()))
        insertRefreshToken(RefreshToken(user = firstUser, token = "first-2", expiresAt = future()))
        insertRefreshToken(RefreshToken(user = secondUser, token = "second-1", expiresAt = future()))

        val updatedCount = refreshTokenRepository.revokeAllByUser(firstUser)

        assertEquals(2, updatedCount)
        assertTrue(refreshTokenRepository.findByToken("first-1").get().isRevoked)
        assertTrue(refreshTokenRepository.findByToken("first-2").get().isRevoked)
        assertFalse(refreshTokenRepository.findByToken("second-1").get().isRevoked)
    }

    @Test
    fun `deleteExpiredAndRevoked deletes expired and revoked tokens only`() {
        val user = insertUser(email = "cleanup@example.com", username = "cleanup")
        insertRefreshToken(RefreshToken(user = user, token = "active", expiresAt = future()))
        insertRefreshToken(RefreshToken(user = user, token = "expired", expiresAt = past()))
        insertRefreshToken(RefreshToken(user = user, token = "revoked", expiresAt = future(), isRevoked = true))

        val deletedCount = refreshTokenRepository.deleteExpiredAndRevoked(System.currentTimeMillis())

        assertEquals(2, deletedCount)
        assertTrue(refreshTokenRepository.findByToken("active").isPresent)
        assertFalse(refreshTokenRepository.findByToken("expired").isPresent)
        assertFalse(refreshTokenRepository.findByToken("revoked").isPresent)
    }

    private fun future(): Long = System.currentTimeMillis() + 60_000

    private fun past(): Long = System.currentTimeMillis() - 60_000

    private fun insertUser(email: String, username: String): User {
        val user = testUser(email = email, username = username)
        assertDoesNotThrow {
            jdbcTemplate.update(
                """
                INSERT INTO users (
                    id, email, username, password_hash, first_name, last_name,
                    language_preference, onboarding_status, is_active, is_email_verified,
                    roles, auth_provider, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                user.id,
                user.email,
                user.username,
                user.passwordHash,
                user.firstName,
                user.lastName,
                user.languagePreference,
                user.onboardingStatus.name,
                user.isActive,
                user.isEmailVerified,
                user.roles,
                user.authProvider.name,
                user.createdAt,
                user.updatedAt,
            )
        }
        return user
    }

    private fun insertRefreshToken(refreshToken: RefreshToken): RefreshToken {
        jdbcTemplate.update(
            """
            INSERT INTO refresh_tokens (
                id, user_id, token, expires_at, is_revoked, created_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            refreshToken.id,
            refreshToken.user.id,
            refreshToken.token,
            refreshToken.expiresAt,
            refreshToken.isRevoked,
            refreshToken.createdAt,
        )
        return refreshToken
    }

    private fun testUser(email: String, username: String): User =
        User(
            email = email,
            username = username,
            passwordHash = "encoded-password",
            firstName = "Test",
            lastName = "User",
            isEmailVerified = true,
        )
}
