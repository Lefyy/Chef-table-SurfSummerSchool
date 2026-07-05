package ru.cheftable.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered
import ru.cheftable.infrastructure.logging.CorrelationIdFilter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.http.HttpStatus
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import ru.cheftable.infrastructure.security.BearerTokenAuthenticationFilter

@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity, bearerFilter: BearerTokenAuthenticationFilter): SecurityFilterChain = http
        .csrf { csrf ->
            csrf.ignoringRequestMatchers(
                AntPathRequestMatcher("/api/**"),
                AntPathRequestMatcher("/slots/*/booking", "POST"),
                AntPathRequestMatcher("/ratings/*", "POST"),
                AntPathRequestMatcher("/logout", "POST"),
            )
            csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/", "/css/**", "/js/**", "/actuator/health", "/error", "/login", "/login/code", "/api/v1/auth/sms/request", "/api/v1/auth/sms/verify").permitAll()
                .anyRequest().authenticated()
        }
        .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter::class.java)
        .exceptionHandling { exceptions ->
            exceptions.authenticationEntryPoint { request, response, _ ->
                val acceptsHtml = request.getHeader("Accept")?.contains("text/html") == true
                if (acceptsHtml) {
                    val target = request.requestURI + (request.queryString?.let { "?$it" } ?: "")
                    response.sendRedirect("/login?redirect=" + java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8))
                } else response.sendError(HttpStatus.UNAUTHORIZED.value())
            }
        }
        .logout { it.disable() }
        .build()

    @Bean
    fun correlationIdFilterRegistration(filter: CorrelationIdFilter): FilterRegistrationBean<CorrelationIdFilter> =
        FilterRegistrationBean(filter).apply {
            order = Ordered.HIGHEST_PRECEDENCE
        }
}
