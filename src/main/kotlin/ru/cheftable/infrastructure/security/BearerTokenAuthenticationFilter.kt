package ru.cheftable.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.cheftable.application.auth.AuthService
import ru.cheftable.web.page.AUTH_COOKIE_NAME

@Component
class BearerTokenAuthenticationFilter(private val authService: AuthService) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val token = request.getHeader("Authorization")?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
            ?: request.cookies?.firstOrNull { it.name == AUTH_COOKIE_NAME }?.value?.takeIf { it.isNotBlank() }
        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            authService.authenticate(token)?.let { client ->
                SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(client, token, listOf(SimpleGrantedAuthority("ROLE_CLIENT")))
            }
        }
        filterChain.doFilter(request, response)
    }
}
