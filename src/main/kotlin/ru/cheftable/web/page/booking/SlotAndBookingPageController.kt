package ru.cheftable.web.page.booking

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.application.booking.*
import ru.cheftable.persistence.SlotJpaRepository
import ru.cheftable.application.booking.NoSeatsException
import ru.cheftable.application.booking.StudioCancelledException
import ru.cheftable.application.booking.ValidationException
import ru.cheftable.application.booking.NotFoundException
import java.util.UUID

@Controller
class SlotAndBookingPageController(
    private val slots: SlotJpaRepository,
    private val bookingOptions: BookingOptionsService,
    private val bookingCreation: BookingCreationService,
) {
    @GetMapping("/slots/{slotId}")
    fun details(@PathVariable slotId: UUID, model: Model): String {
        model.addAttribute("slot", slots.findWithDetailsById(slotId) ?: throw NotFoundException("Slot not found"))
        return "slot/details"
    }

    @GetMapping("/slots/{slotId}/booking")
    fun booking(@PathVariable slotId: UUID, @AuthenticationPrincipal client: AuthenticatedClient, model: Model): String {
        model.addAttribute("slot", slots.findWithDetailsById(slotId) ?: throw NotFoundException("Slot not found"))
        model.addAttribute("options", bookingOptions.options(slotId, client))
        model.addAttribute("form", BookingForm())
        return "booking/create"
    }

    @PostMapping("/slots/{slotId}/booking")
    fun create(@PathVariable slotId: UUID, @AuthenticationPrincipal client: AuthenticatedClient, @ModelAttribute form: BookingForm, redirect: RedirectAttributes): String {
        return try {
            val booking = bookingCreation.create(client, CreateBookingCommand(slotId, form.rentalItemIds, form.allergenIds))
            redirect.addFlashAttribute("bookingId", booking.id)
            "redirect:/booking/success"
        } catch (ex: NoSeatsException) {
            redirect.addFlashAttribute("error", "Мест уже нет. Пожалуйста, выберите другой класс.")
            "redirect:/schedule"
        } catch (ex: StudioCancelledException) {
            redirect.addFlashAttribute("error", "Класс отменён студией. Повторная запись недоступна.")
            "redirect:/schedule"
        } catch (ex: NotFoundException) {
            redirect.addFlashAttribute("error", "Класс не найден.")
            "redirect:/schedule"
        } catch (ex: ValidationException) {
            redirect.addFlashAttribute("error", ex.message ?: "Не удалось создать бронь.")
            "redirect:/slots/$slotId/booking"
        }
    }

    @GetMapping("/booking/success")
    fun success() = "booking/success"
}

data class BookingForm(var rentalItemIds: List<UUID> = emptyList(), var allergenIds: List<UUID> = emptyList())
