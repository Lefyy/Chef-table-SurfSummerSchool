package ru.cheftable.web.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.application.booking.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class BookingApiController(
    private val creation: BookingCreationService,
    private val query: BookingQueryService,
    private val cancellation: BookingCancellationService,
    private val profile: ProfileService,
    private val rating: RatingService,
) {
    @GetMapping("/profile")
    fun profile(@AuthenticationPrincipal client: AuthenticatedClient): ProfileDto = ProfileDto(client.id, client.phone)

    @GetMapping("/profile/bookings")
    fun bookings(@AuthenticationPrincipal client: AuthenticatedClient): List<BookingDto> = query.history(client).map { BookingDto(requireNotNull(it.id), requireNotNull(it.slot?.id), it.slot?.program?.title.orEmpty(), it.status.name, it.paymentStatus.name, it.createdAt) }

    @GetMapping("/profile/bookings/{id}")
    fun bookingDetails(@AuthenticationPrincipal client: AuthenticatedClient, @PathVariable id: UUID): BookingDetailsDto = query.details(client, id).let { details ->
        val b = details.booking
        BookingDetailsDto(requireNotNull(b.id), requireNotNull(b.slot?.id), b.slot?.program?.title.orEmpty(), b.status.name, b.paymentStatus.name, b.totalPriceCents, details.canCancel, details.canRate, details.hasRating)
    }

    @PostMapping("/bookings")
    fun create(@AuthenticationPrincipal client: AuthenticatedClient, @Valid @RequestBody request: CreateBookingDto): BookingDetailsDto {
        val booking = creation.create(client, CreateBookingCommand(request.slotId, request.rentalItemIds, request.allergenIds))
        return bookingDetails(client, requireNotNull(booking.id))
    }

    @PostMapping("/profile/bookings/{id}/cancel")
    fun cancel(@AuthenticationPrincipal client: AuthenticatedClient, @PathVariable id: UUID): BookingDetailsDto {
        cancellation.cancel(client, id)
        return bookingDetails(client, id)
    }

    @GetMapping("/profile/allergies")
    fun allergies(@AuthenticationPrincipal client: AuthenticatedClient): List<AllergyDto> = profile.savedAllergies(client).map { AllergyDto(it.id, it.name) }

    @PutMapping("/profile/allergies")
    fun updateAllergies(@AuthenticationPrincipal client: AuthenticatedClient, @RequestBody request: UpdateAllergiesDto) = profile.updateAllergies(client, request.allergenIds)

    @PostMapping("/ratings/{bookingId}")
    fun rate(@AuthenticationPrincipal client: AuthenticatedClient, @PathVariable bookingId: UUID, @Valid @RequestBody request: RatingRequestDto): RatingDto =
        rating.rate(client, bookingId, request.stars, request.comment).let { RatingDto(requireNotNull(it.id), bookingId, it.stars) }
}

data class ProfileDto(val id: UUID, val phone: String)
data class BookingDto(val id: UUID, val slotId: UUID, val title: String, val status: String, val paymentStatus: String, val createdAt: java.time.OffsetDateTime)
data class BookingDetailsDto(val id: UUID, val slotId: UUID, val title: String, val status: String, val paymentStatus: String, val totalPriceCents: Int, val canCancel: Boolean, val canRate: Boolean, val hasRating: Boolean)
data class CreateBookingDto(val slotId: UUID, val rentalItemIds: List<UUID> = emptyList(), val allergenIds: List<UUID> = emptyList())
data class UpdateAllergiesDto(val allergenIds: List<UUID> = emptyList())
data class AllergyDto(val id: UUID, val name: String)
data class RatingRequestDto(@field:Min(1) @field:Max(5) val stars: Int, val comment: String? = null)
data class RatingDto(val id: UUID, val bookingId: UUID, val stars: Int)
