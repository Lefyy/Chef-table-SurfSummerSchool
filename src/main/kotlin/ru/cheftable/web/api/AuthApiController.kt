package ru.cheftable.web.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ru.cheftable.application.auth.AuthService
import ru.cheftable.application.auth.AuthenticatedClient

@RestController
@RequestMapping("/api/v1/auth")
class AuthApiController(private val authService: AuthService) {
    @PostMapping("/sms/request")
    fun requestSms(@Valid @RequestBody request: SmsRequestDto): SmsRequestResponseDto = authService.requestSms(request.phone).let {
        SmsRequestResponseDto(it.expiresInSeconds, it.resendAvailableInSeconds, it.maxVerificationAttempts, it.maxResendAttempts)
    }

    @PostMapping("/sms/verify")
    fun verifySms(@Valid @RequestBody request: SmsVerifyDto): AuthResponseDto = authService.verifySms(request.phone, request.code).let {
        AuthResponseDto(it.accessToken, it.clientId.toString(), it.phone)
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader(name = "Authorization", required = false) authorization: String?, @AuthenticationPrincipal client: AuthenticatedClient?) {
        val token = authorization?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
        if (token != null) authService.logout(token)
    }
}

data class SmsRequestDto(@field:Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Введите телефон в международном формате") val phone: String)
data class SmsVerifyDto(@field:Pattern(regexp = "^\\+?[0-9]{10,15}$") val phone: String, @field:Pattern(regexp = "^\\d{4}$", message = "Код состоит из 4 цифр") val code: String)
data class SmsRequestResponseDto(val expiresInSeconds: Int, val resendAvailableInSeconds: Int, val maxVerificationAttempts: Int, val maxResendAttempts: Int)
data class AuthResponseDto(val accessToken: String, val clientId: String, val phone: String)
