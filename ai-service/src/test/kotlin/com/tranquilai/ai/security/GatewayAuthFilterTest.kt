package com.tranquilai.ai.security

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

class GatewayAuthFilterTest {

    private val filter = GatewayAuthFilter()

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `authenticates request with gateway headers`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-User-Id", "user-123")
            addHeader("X-User-Email", "user@example.com")
            addHeader("X-User-Roles", "ROLE_USER,ROLE_ADMIN")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth.principal as GatewayUser
        assertEquals("user-123", principal.userId)
        assertEquals("user@example.com", principal.email)
        assertEquals(listOf("ROLE_USER", "ROLE_ADMIN"), auth.authorities.map { it.authority })
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `does not authenticate when user id header is missing`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }
}
