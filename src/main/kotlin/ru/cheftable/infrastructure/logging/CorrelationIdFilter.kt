package ru.cheftable.infrastructure.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

const val CORRELATION_ID_HEADER = "X-Correlation-Id"
const val CORRELATION_ID_MDC_KEY = "correlation_id"

@Component
class CorrelationIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val correlationId = request.getHeader(CORRELATION_ID_HEADER)
            ?.takeIf { it.isNotBlank() }
            ?.take(128)
            ?: UUID.randomUUID().toString()
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
        response.setHeader(CORRELATION_ID_HEADER, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY)
        }
    }
}

fun maskPhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    if (digits.length <= 4) return "****"
    return "+${"*".repeat((digits.length - 4).coerceAtLeast(0))}${digits.takeLast(4)}"
}
