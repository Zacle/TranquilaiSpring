package com.tranquilai.user.security

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class GatewayAuthFilterTest {

    private val filter = GatewayAuthFilter()

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `authenticates request from gateway user headers`() {
        val userId = UUID.randomUUID()
        val request = MockHttpServletRequest().apply {
            addHeader("X-User-Id", userId.toString())
            addHeader("X-User-Email", "user@example.com")
            addHeader("X-User-Roles", "USER,ADMIN")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth.principal as GatewayUser
        assertEquals(userId, principal.id)
        assertEquals("user@example.com", principal.email)
        assertTrue(auth.authorities.map { it.authority }.containsAll(listOf("ROLE_USER", "ROLE_ADMIN")))
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `uses USER role when role header is missing`() {
        val userId = UUID.randomUUID()
        val request = MockHttpServletRequest().apply {
            addHeader("X-User-Id", userId.toString())
            addHeader("X-User-Email", "user@example.com")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals(listOf("ROLE_USER"), auth.authorities.map { it.authority })
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `does not authenticate invalid user id`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-User-Id", "not-a-uuid")
            addHeader("X-User-Email", "user@example.com")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }
}
