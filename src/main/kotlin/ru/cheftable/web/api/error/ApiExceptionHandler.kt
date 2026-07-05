package ru.cheftable.web.api.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.cheftable.application.auth.UnauthorizedException

@RestControllerAdvice(basePackages = ["ru.cheftable.web.api"])
class ApiExceptionHandler {
    @ExceptionHandler(UnauthorizedException::class)
    fun unauthorized(ex: UnauthorizedException) = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorDto("UNAUTHORIZED", ex.message ?: "Unauthorized"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException) = ResponseEntity.badRequest().body(ErrorDto("VALIDATION_ERROR", "Invalid request"))

    @ExceptionHandler(Exception::class)
    fun generic(ex: Exception) = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorDto("SERVER_ERROR", "Internal server error"))
}

data class ErrorDto(val code: String, val message: String)
