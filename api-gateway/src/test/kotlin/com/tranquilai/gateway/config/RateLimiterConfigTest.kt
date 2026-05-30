package com.tranquilai.gateway.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.test.StepVerifier
import java.net.InetSocketAddress

class RateLimiterConfigTest {

    private val config = RateLimiterConfig()
    private val keyResolver = config.remoteAddrKeyResolver()

    @Test
    fun `uses remote address host as rate limit key`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/auth/login")
                .remoteAddress(InetSocketAddress("192.168.1.20", 50321)),
        )

        StepVerifier.create(keyResolver.resolve(exchange))
            .assertNext { key -> assertEquals("192.168.1.20", key) }
            .verifyComplete()
    }

    @Test
    fun `uses unknown when remote address is absent`() {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login"))

        StepVerifier.create(keyResolver.resolve(exchange))
            .assertNext { key -> assertEquals("unknown", key) }
            .verifyComplete()
    }

    @Test
    fun `uses user id header as authenticated rate limit key`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/conversations/conv-1/messages")
                .header("X-User-Id", "user-123")
                .remoteAddress(InetSocketAddress("192.168.1.20", 50321)),
        )

        StepVerifier.create(config.userOrRemoteAddrKeyResolver().resolve(exchange))
            .assertNext { key -> assertEquals("user-123", key) }
            .verifyComplete()
    }

    @Test
    fun `authenticated rate limit key falls back to remote address`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/conversations/conv-1/messages")
                .remoteAddress(InetSocketAddress("192.168.1.20", 50321)),
        )

        StepVerifier.create(config.userOrRemoteAddrKeyResolver().resolve(exchange))
            .assertNext { key -> assertEquals("192.168.1.20", key) }
            .verifyComplete()
    }
}
