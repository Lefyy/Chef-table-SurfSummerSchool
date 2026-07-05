package ru.cheftable.web.page

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.persistence.SlotJpaRepository
import java.time.OffsetDateTime

@Controller
class SchedulePageController(private val slots: SlotJpaRepository) {
    @GetMapping("/schedule")
    fun schedule(
        @AuthenticationPrincipal client: AuthenticatedClient,
        @RequestParam(required = false) level: String?,
        @RequestParam(required = false) seats: Int?,
        model: Model,
    ): String {
        val from = OffsetDateTime.now()
        val filteredSlots = slots.findByStartsAtBetweenOrderByStartsAtAsc(from, from.plusDays(7))
            .filter { level.isNullOrBlank() || it.program?.difficulty?.name == level }
            .filter { seats == null || it.capacity - it.bookedSeats >= seats }
        model.addAttribute("phone", client.phone)
        model.addAttribute("slots", filteredSlots)
        model.addAttribute("selectedLevel", level.orEmpty())
        model.addAttribute("selectedSeats", seats)
        return "schedule/index"
    }
}
