package com.tranquilai.user.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authenticates internal service-to-service calls via X-Internal-Key header.
 * Used by auth-service to create users after registration.
 */
class InternalServiceKeyFilter(
    private val internalServiceKey: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val key = request.getHeader("X-Internal-Key")
        if (key != null && key == internalServiceKey) {
            val auth = UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                listOf(SimpleGrantedAuthority("ROLE_INTERNAL")),
            )
            SecurityContextHolder.getContext().authentication = auth
        }
        filterChain.doFilter(request, response)
    }
}
