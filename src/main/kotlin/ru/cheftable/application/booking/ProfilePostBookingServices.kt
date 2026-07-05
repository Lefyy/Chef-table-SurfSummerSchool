package ru.cheftable.application.booking

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ProfileService(
    private val clients: ClientJpaRepository,
    private val allergens: AllergenJpaRepository,
    private val bookingLinks: BookingLinkRepository,
) {
    @Transactional(readOnly = true)
    fun profile(client: AuthenticatedClient): ClientEntity = clients.findById(client.id).orElseThrow { NotFoundException("Client not found") }

    @Transactional(readOnly = true)
    fun savedAllergies(client: AuthenticatedClient): List<AllergenSelection> = bookingLinks.findClientAllergens(client.id)

    @Transactional(readOnly = true)
    fun allergySettings(client: AuthenticatedClient): AllergySettings {
        val savedAllergenIds = bookingLinks.findAllergenIdsByClientId(client.id).toSet()
        val allergenEntities = allergens.findAllByOrderByNameAsc()
        return AllergySettings(
            allergens = allergenEntities,
            savedAllergenIds = savedAllergenIds,
            allergenOptions = allergenEntities.map { AllergenOption(requireNotNull(it.id), it.name, savedAllergenIds.contains(it.id)) },
        )
    }

    @Transactional
    fun updateAllergies(client: AuthenticatedClient, allergenIds: List<UUID>) {
        val selectedIds = allergenIds.distinct()
        if (selectedIds.isNotEmpty() && allergens.countByIdIn(selectedIds) != selectedIds.size.toLong()) throw ValidationException("Allergen not found")
        bookingLinks.removeClientAllergens(client.id)
        selectedIds.forEach { bookingLinks.addClientAllergen(client.id, it) }
    }
}

@Service
class BookingQueryService(
    private val bookings: BookingJpaRepository,
    private val bookingLinks: BookingLinkRepository,
    private val ratings: RatingJpaRepository,
) {
    @Transactional(readOnly = true)
    fun history(client: AuthenticatedClient): List<BookingEntity> = bookings.findByClientIdOrderByCreatedAtDesc(client.id)

    @Transactional(readOnly = true)
    fun details(client: AuthenticatedClient, bookingId: UUID): BookingDetails {
        val booking = bookings.findWithDetailsById(bookingId) ?: throw NotFoundException("Booking not found")
        if (booking.client?.id != client.id) throw NotFoundException("Booking not found")
        return BookingDetails(
            booking = booking,
            rentals = bookingLinks.findBookingRentals(bookingId),
            allergens = bookingLinks.findBookingAllergens(bookingId),
            canCancel = canCancel(booking),
            canRate = canRate(booking) && ratings.findByBookingId(bookingId) == null,
            hasRating = ratings.findByBookingId(bookingId) != null,
        )
    }

    fun canCancel(booking: BookingEntity): Boolean = booking.status == BookingStatusEntity.ACTIVE &&
        booking.slot?.status != SlotStatusEntity.CANCELLED_BY_STUDIO &&
        booking.slot?.startsAt?.isAfter(OffsetDateTime.now().plusHours(12)) == true

    fun canRate(booking: BookingEntity): Boolean = booking.attended &&
        booking.status in setOf(BookingStatusEntity.ACTIVE, BookingStatusEntity.COMPLETED) &&
        booking.slot?.endsAt?.isBefore(OffsetDateTime.now()) == true
}

@Service
class BookingCancellationService(private val bookings: BookingJpaRepository) {
    @Transactional
    fun cancel(client: AuthenticatedClient, bookingId: UUID): BookingEntity {
        val booking = bookings.findWithDetailsById(bookingId) ?: throw NotFoundException("Booking not found")
        if (booking.client?.id != client.id) throw NotFoundException("Booking not found")
        if (booking.status != BookingStatusEntity.ACTIVE) throw ValidationException("Booking is not active")
        if (booking.slot?.status == SlotStatusEntity.CANCELLED_BY_STUDIO) throw StudioCancelledException("Slot was cancelled by studio")
        if (booking.slot?.startsAt?.isAfter(OffsetDateTime.now().plusHours(12)) != true) throw ValidationException("Cancellation is available only more than 12 hours before class")
        booking.status = BookingStatusEntity.CANCELLED_BY_CLIENT
        booking.cancelledAt = OffsetDateTime.now()
        booking.slot?.bookedSeats = (booking.slot?.bookedSeats ?: 1) - 1
        return booking
    }
}

@Service
class RatingService(
    private val bookings: BookingJpaRepository,
    private val ratings: RatingJpaRepository,
    private val bookingLinks: BookingLinkRepository,
) {
    @Transactional
    fun rate(client: AuthenticatedClient, bookingId: UUID, stars: Int, comment: String?): RatingEntity {
        if (stars !in 1..5) throw ValidationException("Stars must be between 1 and 5")
        val booking = bookings.findWithDetailsById(bookingId) ?: throw NotFoundException("Booking not found")
        if (booking.client?.id != client.id) throw NotFoundException("Booking not found")
        if (!booking.attended) throw ValidationException("Only attended classes can be rated")
        if (booking.slot?.endsAt?.isBefore(OffsetDateTime.now()) != true) throw ValidationException("Class is not completed yet")
        if (ratings.findByBookingId(bookingId) != null) throw ValidationException("Booking already rated")
        val chef = booking.slot?.chef ?: throw ValidationException("Chef not found")
        val rating = ratings.save(RatingEntity(UUID.randomUUID(), booking, chef, booking.client, stars, comment?.trim()?.take(1000), OffsetDateTime.now()))
        bookingLinks.updateChefAverageRating(requireNotNull(chef.id))
        return rating
    }
}

data class BookingDetails(val booking: BookingEntity, val rentals: List<RentalSelection>, val allergens: List<AllergenSelection>, val canCancel: Boolean, val canRate: Boolean, val hasRating: Boolean)
data class RentalSelection(val id: UUID, val name: String, val priceCents: Int, val quantity: Int)
data class AllergenSelection(val id: UUID, val name: String)

data class AllergySettings(val allergens: List<AllergenEntity>, val savedAllergenIds: Set<UUID>, val allergenOptions: List<AllergenOption>)
