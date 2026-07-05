package ru.cheftable.web.page

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.persistence.SlotJpaRepository
import java.time.OffsetDateTime

@Controller
class SchedulePageController(private val slots: SlotJpaRepository) {
    @GetMapping("/schedule")
    fun schedule(@AuthenticationPrincipal client: AuthenticatedClient, model: Model): String {
        val from = OffsetDateTime.now()
        model.addAttribute("phone", client.phone)
        model.addAttribute("slots", slots.findByStartsAtBetweenOrderByStartsAtAsc(from, from.plusDays(7)))
        return "schedule/index"
    }
}
