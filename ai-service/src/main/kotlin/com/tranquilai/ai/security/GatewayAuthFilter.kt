package com.tranquilai.ai.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

data class GatewayUser(
    val userId: String,
    val email: String,
    val roles: List<String>,
)

@Component
class GatewayAuthFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userId = request.getHeader("X-User-Id")
        val email = request.getHeader("X-User-Email") ?: ""
        val rolesHeader = request.getHeader("X-User-Roles") ?: ""

        if (!userId.isNullOrBlank()) {
            val roles = rolesHeader.split(",").filter { it.isNotBlank() }.map { SimpleGrantedAuthority(it.trim()) }
            val user = GatewayUser(userId = userId, email = email, roles = rolesHeader.split(",").filter { it.isNotBlank() })
            val auth = UsernamePasswordAuthenticationToken(user, null, roles)
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
