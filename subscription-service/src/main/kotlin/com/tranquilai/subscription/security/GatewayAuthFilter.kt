package com.tranquilai.subscription.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest
import java.util.UUID

@Component
class GatewayAuthFilter(
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // Internal service-to-service calls
        val providedKey = request.getHeader("X-Internal-Key")
        if (providedKey != null && safeEquals(providedKey, internalKey)) {
            val auth = UsernamePasswordAuthenticationToken(
                "internal-service", null,
                listOf(SimpleGrantedAuthority("ROLE_INTERNAL")),
            )
            SecurityContextHolder.getContext().authentication = auth
            filterChain.doFilter(request, response)
            return
        }

        // Gateway-injected user headers
        val userId = request.getHeader("X-User-Id")
        val userEmail = request.getHeader("X-User-Email")
        val userRoles = request.getHeader("X-User-Roles") ?: "USER"

        if (userId != null && userEmail != null) {
            runCatching { UUID.fromString(userId) }.getOrNull()?.let { id ->
                val authorities = userRoles.split(",")
                    .map { SimpleGrantedAuthority("ROLE_${it.trim()}") }
                val principal = GatewayUser(id = id, email = userEmail, roles = userRoles)
                val auth = UsernamePasswordAuthenticationToken(principal, null, authorities)
                SecurityContextHolder.getContext().authentication = auth
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun safeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}

data class GatewayUser(
    val id: UUID,
    val email: String,
    val roles: String,
)
