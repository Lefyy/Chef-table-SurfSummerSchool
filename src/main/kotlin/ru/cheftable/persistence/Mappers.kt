package ru.cheftable.persistence

import ru.cheftable.domain.*

fun ClientEntity.toDomain() = Client(requireNotNull(id), PhoneNumber(phone))
fun ChefEntity.toDomain() = Chef(requireNotNull(id), name, bio)
fun ProgramEntity.toDomain() = Program(requireNotNull(id), title, description, DifficultyLevel.valueOf(difficulty.name), durationMinutes, priceCents)
fun SlotEntity.toDomain() = Slot(requireNotNull(id), requireNotNull(program).toDomain(), requireNotNull(chef).toDomain(), startsAt, endsAt, capacity, bookedSeats, SlotStatus.valueOf(status.name))
fun RentalItemEntity.toDomain() = RentalItem(requireNotNull(id), name, priceCents, stock)
fun AllergenEntity.toDomain() = Allergen(requireNotNull(id), code, name)
fun BookingEntity.toDomain() = Booking(requireNotNull(id), requireNotNull(client).id!!, requireNotNull(slot).toDomain(), BookingStatus.valueOf(status.name), PaymentStatus.valueOf(paymentStatus.name), totalPriceCents, attended, createdAt)
fun RatingEntity.toDomain() = Rating(requireNotNull(id), requireNotNull(booking).id!!, requireNotNull(chef).id!!, requireNotNull(client).id!!, stars, comment)
