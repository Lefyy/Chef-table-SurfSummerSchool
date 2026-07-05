package ru.cheftable.application.booking

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BookingOptionsService(
    private val slots: SlotJpaRepository,
    private val rentals: RentalItemJpaRepository,
    private val allergens: AllergenJpaRepository,
    private val bookingLinks: BookingLinkRepository,
) {
    @Transactional(readOnly = true)
    fun options(slotId: UUID, client: AuthenticatedClient): BookingOptions {
        val slot = slots.findWithDetailsById(slotId) ?: throw NotFoundException("Slot not found")
        validateBookable(slot)
        val savedAllergenIds = bookingLinks.findAllergenIdsByClientId(client.id).toSet()
        val allergenEntities = allergens.findAllByOrderByNameAsc()
        val allergenOptions = allergenEntities.map { AllergenOption(requireNotNull(it.id), it.name, savedAllergenIds.contains(it.id)) }
        return BookingOptions(rentals.findByActiveTrueAndStockGreaterThanOrderByNameAsc(), allergenEntities, savedAllergenIds, allergenOptions)
    }
}

@Service
class BookingCreationService(
    private val slots: SlotJpaRepository,
    private val clients: ClientJpaRepository,
    private val bookings: BookingJpaRepository,
    private val rentals: RentalItemJpaRepository,
    private val allergens: AllergenJpaRepository,
    private val bookingLinks: BookingLinkRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun create(client: AuthenticatedClient, command: CreateBookingCommand): BookingEntity {
        val slot = slots.findLockedWithDetailsById(command.slotId) ?: throw NotFoundException("Slot not found")
        validateBookable(slot)
        val rentalItems = command.selectedRentalItemIds.distinct().map { rentals.findById(it).orElseThrow { ValidationException("Rental item not found") } }
        if (rentalItems.any { !it.active || it.stock <= 0 }) throw ValidationException("Rental item is unavailable")
        val selectedAllergenIds = command.selectedAllergenIds.distinct()
        if (selectedAllergenIds.isNotEmpty() && allergens.countByIdIn(selectedAllergenIds) != selectedAllergenIds.size.toLong()) throw ValidationException("Allergen not found")

        slot.bookedSeats += 1
        val total = (slot.program?.priceCents ?: 0) + rentalItems.sumOf { it.priceCents }
        val booking = bookings.saveAndFlush(BookingEntity(UUID.randomUUID(), clients.getReferenceById(client.id), slot, BookingStatusEntity.ACTIVE, PaymentStatusEntity.NOT_REQUIRED, total, false, OffsetDateTime.now(), null))
        rentalItems.forEach { bookingLinks.addRental(requireNotNull(booking.id), requireNotNull(it.id), 1) }
        selectedAllergenIds.forEach {
            bookingLinks.addBookingAllergen(requireNotNull(booking.id), it)
            bookingLinks.addClientAllergen(client.id, it)
        }
        log.info("booking_created event_type=booking_created booking_id={} client_id={} slot_id={}", booking.id, client.id, slot.id)
        return bookings.findWithDetailsById(requireNotNull(booking.id)) ?: booking
    }
}

fun validateBookable(slot: SlotEntity) {
    if (slot.status == SlotStatusEntity.CANCELLED_BY_STUDIO) throw StudioCancelledException("Slot was cancelled by studio")
    if (slot.capacity - slot.bookedSeats <= 0) throw NoSeatsException("No seats available")
    if (slot.startsAt.isBefore(OffsetDateTime.now())) throw ValidationException("Slot has already started")
}

data class BookingOptions(val rentalItems: List<RentalItemEntity>, val allergens: List<AllergenEntity>, val savedAllergenIds: Set<UUID>, val allergenOptions: List<AllergenOption>)
data class AllergenOption(val id: UUID, val name: String, val selected: Boolean)
data class CreateBookingCommand(val slotId: UUID, val selectedRentalItemIds: List<UUID>, val selectedAllergenIds: List<UUID>)
class NotFoundException(message: String) : RuntimeException(message)
class ValidationException(message: String) : RuntimeException(message)
class NoSeatsException(message: String) : RuntimeException(message)
class StudioCancelledException(message: String) : RuntimeException(message)
