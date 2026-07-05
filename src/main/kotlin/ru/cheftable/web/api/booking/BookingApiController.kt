package ru.cheftable.web.api.booking

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.application.booking.BookingCreationService
import ru.cheftable.application.booking.CreateBookingCommand
import ru.cheftable.persistence.BookingEntity
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/bookings")
class BookingApiController(private val bookingCreation: BookingCreationService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal client: AuthenticatedClient, @Valid @RequestBody request: CreateBookingRequest): BookingDto =
        bookingCreation.create(client, CreateBookingCommand(request.slotId, request.selectedRentalItemIds, request.selectedAllergenIds)).toDto()
}

data class CreateBookingRequest(
    @field:NotNull val slotId: UUID,
    val selectedRentalItemIds: List<UUID> = emptyList(),
    val selectedAllergenIds: List<UUID> = emptyList(),
)

data class BookingDto(val id: String, val slotId: String, val status: String, val paymentStatus: String, val totalPriceCents: Int, val createdAt: OffsetDateTime)
fun BookingEntity.toDto() = BookingDto(id.toString(), slot?.id.toString(), status.name, paymentStatus.name, totalPriceCents, createdAt)
