package com.tranquilai.plan.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class InternalServiceKeyFilter(
    private val internalServiceKey: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val key = request.getHeader("X-Internal-Key")
        if (isValidInternalKey(key)) {
            val auth = UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                listOf(SimpleGrantedAuthority("ROLE_INTERNAL")),
            )
            SecurityContextHolder.getContext().authentication = auth
        }
        filterChain.doFilter(request, response)
    }

    private fun isValidInternalKey(key: String?): Boolean {
        if (key == null) return false
        return MessageDigest.isEqual(
            key.toByteArray(StandardCharsets.UTF_8),
            internalServiceKey.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
