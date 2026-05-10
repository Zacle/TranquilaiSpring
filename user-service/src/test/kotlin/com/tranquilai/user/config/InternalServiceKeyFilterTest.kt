package com.tranquilai.user.config

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

class InternalServiceKeyFilterTest {

    private val filter = InternalServiceKeyFilter("internal-secret")

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `authenticates internal service when key matches`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-Internal-Key", "internal-secret")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals("internal-service", auth.principal)
        assertEquals(listOf("ROLE_INTERNAL"), auth.authorities.map { it.authority })
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `does not authenticate internal service when key is missing or wrong`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-Internal-Key", "wrong")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }
}
