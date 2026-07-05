package ru.cheftable.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .authorizeHttpRequests { it.requestMatchers("/", "/css/**", "/actuator/health", "/login").permitAll().anyRequest().authenticated() }
        .formLogin { it.loginPage("/login").permitAll() }
        .logout { it.logoutUrl("/logout").logoutSuccessUrl("/login?logout") }
        .build()
}
