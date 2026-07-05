package ru.cheftable.web.api.auth

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ru.cheftable.application.auth.AuthService
import ru.cheftable.application.auth.AuthenticatedClient

@RestController
@RequestMapping("/api/v1/auth")
class AuthApiController(private val authService: AuthService) {
    @PostMapping("/sms/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestSms(@Valid @RequestBody request: SmsRequestDto) = authService.requestSms(request.phone)

    @PostMapping("/sms/verify")
    fun verifySms(@Valid @RequestBody request: SmsVerifyDto): SmsVerifyResponseDto {
        val result = authService.verifySms(request.phone, request.code)
        return SmsVerifyResponseDto(result.accessToken, "Bearer", ClientProfileDto(result.clientId.toString(), result.phone))
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestHeader("Authorization") authorization: String, @AuthenticationPrincipal client: AuthenticatedClient?) {
        authService.logout(authorization.removePrefix("Bearer "))
    }
}

data class SmsRequestDto(@field:Pattern(regexp = "^\\+?[0-9]{10,15}$") val phone: String)
data class SmsVerifyDto(@field:Pattern(regexp = "^\\+?[0-9]{10,15}$") val phone: String, @field:Pattern(regexp = "^\\d{4}$") val code: String)
data class SmsVerifyResponseDto(val accessToken: String, val tokenType: String, val profile: ClientProfileDto)
data class ClientProfileDto(val id: String, val phone: String)
