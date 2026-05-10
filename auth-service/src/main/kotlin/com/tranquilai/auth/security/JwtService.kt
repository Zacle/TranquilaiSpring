package com.tranquilai.auth.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @param:Value("\${jwt.secret}") private val secret: String,
    @param:Value("\${jwt.access-expiration}") private val accessExpiration: Long,
) {
    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userId: UUID, email: String, roles: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(Date(now.time + accessExpiration))
            .signWith(signingKey)
            .compact()
    }

    fun extractUserId(token: String): UUID =
        UUID.fromString(extractClaims(token).subject)

    fun extractEmail(token: String): String =
        extractClaims(token)["email"] as String

    fun isTokenValid(token: String): Boolean = runCatching {
        extractClaims(token).expiration.after(Date())
    }.getOrDefault(false)

    fun getAccessExpirationMs(): Long = accessExpiration

    private fun extractClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
