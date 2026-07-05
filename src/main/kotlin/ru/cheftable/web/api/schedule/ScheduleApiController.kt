package ru.cheftable.web.api.schedule

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.cheftable.persistence.SlotJpaRepository
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/schedule")
class ScheduleApiController(private val slots: SlotJpaRepository) {
    @GetMapping("/slots")
    fun slots(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateFrom: OffsetDateTime?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateTo: OffsetDateTime?,
    ): SlotsResponseDto {
        val from = dateFrom ?: OffsetDateTime.now()
        val to = dateTo ?: from.plusDays(7)
        return SlotsResponseDto(slots.findByStartsAtBetweenOrderByStartsAtAsc(from, to).map { it.toDto() })
    }

    @GetMapping("/slots/{slotId}")
    fun slot(@PathVariable slotId: UUID): SlotDto = slots.findWithDetailsById(slotId)?.toDto() ?: throw NoSuchElementException("Slot not found")
}

data class SlotsResponseDto(val items: List<SlotDto>)
data class SlotDto(
    val id: String,
    val title: String,
    val description: String,
    val level: String,
    val chefName: String,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val capacity: Int,
    val availableSeats: Int,
    val status: String,
    val priceCents: Int,
)

private fun ru.cheftable.persistence.SlotEntity.toDto(): SlotDto = SlotDto(
    id = id.toString(),
    title = program?.title.orEmpty(),
    description = program?.description.orEmpty(),
    level = program?.difficulty?.name.orEmpty(),
    chefName = chef?.name.orEmpty(),
    startsAt = startsAt,
    endsAt = endsAt,
    capacity = capacity,
    availableSeats = capacity - bookedSeats,
    status = status.name,
    priceCents = program?.priceCents ?: 0,
)
