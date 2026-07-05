package ru.cheftable.web.api.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.cheftable.application.auth.UnauthorizedException
import ru.cheftable.application.booking.NoSeatsException
import ru.cheftable.application.booking.NotFoundException
import ru.cheftable.application.booking.StudioCancelledException
import ru.cheftable.application.booking.ValidationException

@RestControllerAdvice(basePackages = ["ru.cheftable.web.api"])
class ApiExceptionHandler {
    @ExceptionHandler(UnauthorizedException::class)
    fun unauthorized(ex: UnauthorizedException) = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorDto("UNAUTHORIZED", ex.message ?: "Unauthorized"))

    @ExceptionHandler(MethodArgumentNotValidException::class, ValidationException::class)
    fun validation(ex: Exception) = ResponseEntity.badRequest().body(ErrorDto("VALIDATION_ERROR", ex.message ?: "Invalid request"))

    @ExceptionHandler(NotFoundException::class, NoSuchElementException::class)
    fun notFound(ex: Exception) = ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorDto("NOT_FOUND", ex.message ?: "Not found"))

    @ExceptionHandler(NoSeatsException::class)
    fun noSeats(ex: NoSeatsException) = ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorDto("NO_SEATS", ex.message ?: "No seats available"))

    @ExceptionHandler(StudioCancelledException::class)
    fun studioCancelled(ex: StudioCancelledException) = ResponseEntity.status(HttpStatus.GONE).body(ErrorDto("STUDIO_CANCELLED", ex.message ?: "Slot cancelled"))

    @ExceptionHandler(Exception::class)
    fun generic(ex: Exception) = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorDto("SERVER_ERROR", "Internal server error"))
}

data class ErrorDto(val code: String, val message: String)
