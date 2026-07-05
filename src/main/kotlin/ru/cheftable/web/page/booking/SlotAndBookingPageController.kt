package ru.cheftable.web.page.booking

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.application.booking.*
import ru.cheftable.persistence.SlotJpaRepository
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
        val booking = bookingCreation.create(client, CreateBookingCommand(slotId, form.rentalItemIds, form.allergenIds))
        redirect.addFlashAttribute("bookingId", booking.id)
        return "redirect:/booking/success"
    }

    @GetMapping("/booking/success")
    fun success() = "booking/success"
}

data class BookingForm(var rentalItemIds: List<UUID> = emptyList(), var allergenIds: List<UUID> = emptyList())
