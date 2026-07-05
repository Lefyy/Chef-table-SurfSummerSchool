package ru.cheftable.web.error

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.ui.Model
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.annotation.ResponseStatus
import ru.cheftable.application.auth.UnauthorizedException
import ru.cheftable.application.booking.*
import ru.cheftable.infrastructure.logging.CORRELATION_ID_MDC_KEY

@RestControllerAdvice(basePackages = ["ru.cheftable.web.api"])
class ApiExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException) = error(HttpStatus.BAD_REQUEST, "validation_error", ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Некорректные данные")

    @ExceptionHandler(UnauthorizedException::class)
    fun unauthorized() = error(HttpStatus.UNAUTHORIZED, "unauthorized", "Требуется авторизация")

    @ExceptionHandler(NotFoundException::class)
    fun notFound() = error(HttpStatus.NOT_FOUND, "not_found", "Объект не найден")

    @ExceptionHandler(NoSeatsException::class, StudioCancelledException::class)
    fun conflict(ex: RuntimeException) = error(HttpStatus.CONFLICT, "business_conflict", uiMessage(ex))

    @ExceptionHandler(ValidationException::class)
    fun unprocessable(ex: ValidationException) = error(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error", uiMessage(ex))

    @ExceptionHandler(Exception::class)
    fun unexpected(ex: Exception): ResponseEntity<ApiErrorResponse> {
        log.error("unexpected_api_error event_type=unexpected_error", ex)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "server_error", "Внутренняя ошибка сервера")
    }

    private fun error(status: HttpStatus, code: String, message: String) = ResponseEntity.status(status).body(ApiErrorResponse(code, message, MDC.get(CORRELATION_ID_MDC_KEY)))
}

@ControllerAdvice(basePackages = ["ru.cheftable.web.page"])
class PageExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(model: Model): String = errorPage(model, "Не найдено", "Запрошенный объект не найден.")

    @ExceptionHandler(NoSeatsException::class, StudioCancelledException::class, ValidationException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun business(ex: RuntimeException, model: Model): String = errorPage(model, "Действие недоступно", uiMessage(ex))

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun unexpected(ex: Exception, request: HttpServletRequest, model: Model): String {
        log.error("unexpected_page_error event_type=unexpected_error path={}", request.requestURI, ex)
        return errorPage(model, "Ошибка", "Произошла внутренняя ошибка. Попробуйте позже.")
    }

    private fun errorPage(model: Model, title: String, message: String): String {
        model.addAttribute("title", title)
        model.addAttribute("message", message)
        model.addAttribute("correlationId", MDC.get(CORRELATION_ID_MDC_KEY))
        return "error/generic"
    }
}

data class ApiErrorResponse(val code: String, val message: String, val correlationId: String?)

private fun uiMessage(ex: RuntimeException): String = when (ex) {
    is NoSeatsException -> "Свободных мест на этот класс уже нет."
    is StudioCancelledException -> "Класс отменён студией."
    is ValidationException -> when {
        ex.message?.contains("Cancellation", ignoreCase = true) == true -> "Отмена доступна только более чем за 12 часов до начала класса."
        ex.message?.contains("rated", ignoreCase = true) == true -> "Оценка для этой брони уже оставлена."
        ex.message?.contains("Rental", ignoreCase = true) == true -> "Выбранный прокат недоступен."
        ex.message?.contains("Allergen", ignoreCase = true) == true -> "Выбранная аллергия недоступна."
        else -> "Проверьте введённые данные."
    }
    else -> "Действие недоступно."
}
