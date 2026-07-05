package ru.cheftable.web.page.error

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import ru.cheftable.application.booking.NoSeatsException
import ru.cheftable.application.booking.NotFoundException
import ru.cheftable.application.booking.StudioCancelledException
import ru.cheftable.application.booking.ValidationException

@ControllerAdvice(basePackages = ["ru.cheftable.web.page"])
class PageExceptionHandler {

    @ExceptionHandler(NoSeatsException::class)
    fun noSeats(ex: NoSeatsException, redirect: RedirectAttributes): String {
        redirect.addFlashAttribute("error", "Нет свободных мест на этот класс")
        return "redirect:/schedule"
    }

    @ExceptionHandler(StudioCancelledException::class)
    fun studioCancelled(ex: StudioCancelledException, redirect: RedirectAttributes): String {
        redirect.addFlashAttribute("error", "Класс отменён студией")
        return "redirect:/schedule"
    }

    @ExceptionHandler(ValidationException::class)
    fun validation(ex: ValidationException, redirect: RedirectAttributes): String {
        redirect.addFlashAttribute("error", ex.message ?: "Не удалось выполнить операцию")
        return "redirect:/schedule"
    }

    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException, redirect: RedirectAttributes): String {
        redirect.addFlashAttribute("error", "Класс не найден")
        return "redirect:/schedule"
    }
}