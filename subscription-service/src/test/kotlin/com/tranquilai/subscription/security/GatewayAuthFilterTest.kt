package com.tranquilai.subscription.security

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class GatewayAuthFilterTest {

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `authenticates internal service key before gateway headers`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-Internal-Key", "secret")
            addHeader("X-User-Id", UUID.randomUUID().toString())
            addHeader("X-User-Email", "user@example.com")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        GatewayAuthFilter("secret").doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals("internal-service", auth.principal)
        assertEquals(listOf("ROLE_INTERNAL"), auth.authorities.map { it.authority })
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `authenticates gateway user headers`() {
        val userId = UUID.randomUUID()
        val request = MockHttpServletRequest().apply {
            addHeader("X-User-Id", userId.toString())
            addHeader("X-User-Email", "user@example.com")
            addHeader("X-User-Roles", "USER,ADMIN")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        GatewayAuthFilter("secret").doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals(userId, (auth.principal as GatewayUser).id)
        assertEquals(listOf("ROLE_USER", "ROLE_ADMIN"), auth.authorities.map { it.authority })
    }

    @Test
    fun `leaves invalid request unauthenticated`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-Internal-Key", "wrong")
            addHeader("X-User-Id", "bad")
            addHeader("X-User-Email", "user@example.com")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        GatewayAuthFilter("secret").doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
