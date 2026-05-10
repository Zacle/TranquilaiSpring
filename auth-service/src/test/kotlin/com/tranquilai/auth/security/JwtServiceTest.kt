package com.tranquilai.auth.security

import com.tranquilai.auth.entity.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtServiceTest {

    private val secret = "test-secret-that-is-long-enough-for-hs256!!"

    @Test
    fun `generated token contains user id email and roles`() {
        val service = JwtService(secret = secret, accessExpiration = 60_000)
        val userId = UUID.randomUUID()
        val token = service.generateAccessToken(userId, "user@example.com", UserRole.USER.name)

        assertEquals(userId, service.extractUserId(token))
        assertEquals("user@example.com", service.extractEmail(token))
        assertTrue(service.isTokenValid(token))
        assertEquals(60_000, service.getAccessExpirationMs())
    }

    @Test
    fun `expired token is not valid`() {
        val service = JwtService(secret = secret, accessExpiration = -1)
        val token = service.generateAccessToken(UUID.randomUUID(), "user@example.com", UserRole.USER.name)

        assertFalse(service.isTokenValid(token))
    }

    @Test
    fun `malformed token is not valid`() {
        val service = JwtService(secret = secret, accessExpiration = 60_000)

        assertFalse(service.isTokenValid("not-a-jwt"))
    }
}
