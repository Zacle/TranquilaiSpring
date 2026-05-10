package com.tranquilai.notification.security

import com.tranquilai.notification.config.InternalServiceKeyFilter
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

class NotificationSecurityFiltersTest {

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `gateway filter authenticates gateway user headers`() {
        val userId = UUID.randomUUID()
        val request = MockHttpServletRequest().apply {
            addHeader("X-User-Id", userId.toString())
            addHeader("X-User-Email", "user@example.com")
            addHeader("X-User-Roles", "USER,ADMIN")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        GatewayAuthFilter().doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals(userId, (auth.principal as GatewayUser).id)
        assertEquals(listOf("ROLE_USER", "ROLE_ADMIN"), auth.authorities.map { it.authority })
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `gateway filter ignores invalid gateway user id`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-User-Id", "bad")
            addHeader("X-User-Email", "user@example.com")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        GatewayAuthFilter().doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `internal key filter grants internal role for matching key`() {
        val request = MockHttpServletRequest().apply { addHeader("X-Internal-Key", "secret") }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        InternalServiceKeyFilter("secret").doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals("internal-service", auth.principal)
        assertEquals(listOf("ROLE_INTERNAL"), auth.authorities.map { it.authority })
        verify(chain).doFilter(request, response)
    }
}
