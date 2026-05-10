package com.tranquilai.gateway.filter

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

/**
 * Gateway filter that validates JWT tokens and forwards user info to downstream services
 * via X-User-Id and X-User-Email headers.
 */
@Component
class JwtAuthFilter(
    @param:Value("\${jwt.secret}") private val secret: String,
) : AbstractGatewayFilterFactory<JwtAuthFilter.Config>(Config::class.java) {

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    data class Config(val dummy: String = "")

    override fun apply(config: Config): GatewayFilter = GatewayFilter { exchange, chain ->
        val request = exchange.request
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return@GatewayFilter exchange.response.setComplete()
        }

        val token = authHeader.substring(7)
        val claims = extractClaims(token)

        if (claims == null) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return@GatewayFilter exchange.response.setComplete()
        }

        // Forward user identity to downstream services
        val mutatedRequest = request.mutate()
            .header("X-User-Id", claims.subject)
            .header("X-User-Email", claims["email"] as? String ?: "")
            .header("X-User-Roles", claims["roles"] as? String ?: "USER")
            .build()

        chain.filter(exchange.mutate().request(mutatedRequest).build())
    }

    private fun extractClaims(token: String): Claims? = runCatching {
        val claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
        if (claims.expiration.before(java.util.Date())) null else claims
    }.getOrNull()
}
