package com.tranquilai.gateway.filter

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.Date

class JwtAuthFilterTest {

    private val secret = "test-secret-that-is-long-enough-for-hs256!!"
    private val filter = JwtAuthFilter(secret)

    @Test
    fun `returns unauthorized when authorization header is missing`() {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me"))
        val chain = RecordingGatewayFilterChain()

        StepVerifier.create(filter.apply(JwtAuthFilter.Config()).filter(exchange, chain))
            .verifyComplete()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        assertFalse(chain.wasCalled)
    }

    @Test
    fun `returns unauthorized when authorization header is not a bearer token`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic abc123"),
        )
        val chain = RecordingGatewayFilterChain()

        StepVerifier.create(filter.apply(JwtAuthFilter.Config()).filter(exchange, chain))
            .verifyComplete()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        assertFalse(chain.wasCalled)
    }

    @Test
    fun `returns unauthorized when token cannot be parsed`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"),
        )
        val chain = RecordingGatewayFilterChain()

        StepVerifier.create(filter.apply(JwtAuthFilter.Config()).filter(exchange, chain))
            .verifyComplete()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        assertFalse(chain.wasCalled)
    }

    @Test
    fun `returns unauthorized when token is expired`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwt(expiration = Date(System.currentTimeMillis() - 1_000))}"),
        )
        val chain = RecordingGatewayFilterChain()

        StepVerifier.create(filter.apply(JwtAuthFilter.Config()).filter(exchange, chain))
            .verifyComplete()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        assertFalse(chain.wasCalled)
    }

    @Test
    fun `forwards user identity headers for valid token`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwt()}"),
        )
        val chain = RecordingGatewayFilterChain()

        StepVerifier.create(filter.apply(JwtAuthFilter.Config()).filter(exchange, chain))
            .verifyComplete()

        assertTrue(chain.wasCalled)
        assertEquals("user-123", chain.exchange?.request?.headers?.getFirst("X-User-Id"))
        assertEquals("user@example.com", chain.exchange?.request?.headers?.getFirst("X-User-Email"))
        assertEquals("ADMIN", chain.exchange?.request?.headers?.getFirst("X-User-Roles"))
    }

    @Test
    fun `forwards default user role when roles claim is missing`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwt(roles = null)}"),
        )
        val chain = RecordingGatewayFilterChain()

        StepVerifier.create(filter.apply(JwtAuthFilter.Config()).filter(exchange, chain))
            .verifyComplete()

        assertTrue(chain.wasCalled)
        assertEquals("USER", chain.exchange?.request?.headers?.getFirst("X-User-Roles"))
    }

    private fun jwt(
        subject: String = "user-123",
        email: String = "user@example.com",
        roles: String? = "ADMIN",
        expiration: Date = Date(System.currentTimeMillis() + 60_000),
    ): String {
        val builder = Jwts.builder()
            .subject(subject)
            .claim("email", email)
            .expiration(expiration)

        if (roles != null) {
            builder.claim("roles", roles)
        }

        return builder
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()))
            .compact()
    }

    private class RecordingGatewayFilterChain : GatewayFilterChain {
        var wasCalled = false
            private set
        var exchange: ServerWebExchange? = null
            private set

        override fun filter(exchange: ServerWebExchange): Mono<Void> {
            wasCalled = true
            this.exchange = exchange
            return Mono.empty()
        }
    }
}
