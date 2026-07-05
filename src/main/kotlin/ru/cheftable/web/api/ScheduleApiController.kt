package ru.cheftable.web.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.cheftable.application.booking.NotFoundException
import ru.cheftable.persistence.SlotEntity
import ru.cheftable.persistence.SlotJpaRepository
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ScheduleApiController(private val slots: SlotJpaRepository) {
    @GetMapping("/schedule")
    fun schedule(): List<SlotDto> {
        val now = OffsetDateTime.now()
        return slots.findByStartsAtBetweenOrderByStartsAtAsc(now, now.plusDays(7)).map { it.toDto() }
    }

    @GetMapping("/slots/{id}")
    fun slot(@PathVariable id: UUID): SlotDto = (slots.findWithDetailsById(id) ?: throw NotFoundException("Slot not found")).toDto()
}

data class SlotDto(val id: UUID, val title: String, val chef: String, val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val status: String, val freeSeats: Int, val priceCents: Int)
private fun SlotEntity.toDto() = SlotDto(requireNotNull(id), program?.title.orEmpty(), chef?.name.orEmpty(), startsAt, endsAt, status.name, capacity - bookedSeats, program?.priceCents ?: 0)
