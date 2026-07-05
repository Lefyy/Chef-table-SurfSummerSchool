package ru.cheftable.web.page.booking

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.application.booking.*
import java.util.UUID

@Controller
class ProfilePageController(
    private val profileService: ProfileService,
    private val bookingQuery: BookingQueryService,
    private val cancellation: BookingCancellationService,
    private val ratingService: RatingService,
) {
    @GetMapping("/profile")
    fun profile(@AuthenticationPrincipal client: AuthenticatedClient, model: Model): String {
        model.addAttribute("clientProfile", profileService.profile(client))
        model.addAttribute("bookings", bookingQuery.history(client).take(3))
        return "profile/index"
    }

    @GetMapping("/profile/bookings")
    fun history(@AuthenticationPrincipal client: AuthenticatedClient, model: Model): String {
        model.addAttribute("bookings", bookingQuery.history(client))
        return "profile/bookings"
    }

    @GetMapping("/profile/bookings/{bookingId}")
    fun details(@AuthenticationPrincipal client: AuthenticatedClient, @PathVariable bookingId: UUID, model: Model): String {
        model.addAttribute("details", bookingQuery.details(client, bookingId))
        return "profile/booking-details"
    }

    @PostMapping("/profile/bookings/{bookingId}/cancel")
    fun cancel(@AuthenticationPrincipal client: AuthenticatedClient, @PathVariable bookingId: UUID, redirect: RedirectAttributes): String {
        return try {
            cancellation.cancel(client, bookingId)
            redirect.addFlashAttribute("message", "Бронь отменена")
            "redirect:/profile/bookings/$bookingId"
        } catch (ex: ValidationException) {
            redirect.addFlashAttribute("error", "Отмена доступна только более чем за 12 часов до начала класса")
            "redirect:/profile/bookings/$bookingId"
        }
    }

    @GetMapping("/profile/allergies")
    fun allergies(@AuthenticationPrincipal client: AuthenticatedClient, model: Model): String {
        model.addAttribute("allergies", profileService.savedAllergies(client))
        return "profile/allergies"
    }

    @GetMapping("/ratings/{bookingId}")
    fun ratingForm(@AuthenticationPrincipal client: AuthenticatedClient, @PathVariable bookingId: UUID, model: Model): String {
        model.addAttribute("details", bookingQuery.details(client, bookingId))
        model.addAttribute("form", RatingForm())
        return "rating/create"
    }

    @PostMapping("/ratings/{bookingId}")
    fun rate(@AuthenticationPrincipal client: AuthenticatedClient, @PathVariable bookingId: UUID, @ModelAttribute form: RatingForm, redirect: RedirectAttributes): String {
        return try {
            ratingService.rate(client, bookingId, form.stars, form.comment)
            redirect.addFlashAttribute("message", "Спасибо за оценку!")
            "redirect:/profile/bookings/$bookingId"
        } catch (ex: ValidationException) {
            redirect.addFlashAttribute("error", ex.message ?: "Не удалось сохранить оценку")
            "redirect:/ratings/$bookingId"
        }
    }
}

data class RatingForm(var stars: Int = 5, var comment: String? = null)
