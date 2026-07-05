package ru.cheftable.domain

import java.time.OffsetDateTime
import java.util.UUID

@JvmInline value class PhoneNumber(val value: String)

data class Client(
  val id: UUID,
  val phone: PhoneNumber)

data class Chef(
  val id: UUID,
  val name: String,
  val bio: String?)

data class Program(
  val id: UUID,
  val title: String,
  val description: String,
  val difficulty: DifficultyLevel,
  val durationMinutes: Int,
  val priceCents: Int)

enum class DifficultyLevel { BEGINNER, INTERMEDIATE, ADVANCED }

data class Slot(
  val id: UUID,
  val program: Program,
  val chef: Chef,
  val startsAt: OffsetDateTime,
  val endsAt: OffsetDateTime,
  val capacity: Int,
  val bookedSeats: Int,
  val status: SlotStatus) { val availableSeats: Int get() = capacity - bookedSeats }

enum class SlotStatus { SCHEDULED, CANCELLED_BY_STUDIO }

data class RentalItem(
  val id: UUID,
  val name: String,
  val priceCents: Int,
  val stock: Int)

data class Allergen(
  val id: UUID,
  val code: String,
  val name: String)

data class Booking(
  val id: UUID,
  val clientId: UUID,
  val slot: Slot,
  val status: BookingStatus,
  val paymentStatus: PaymentStatus,
  val totalPriceCents: Int,
  val attended: Boolean,
  val createdAt: OffsetDateTime)

enum class BookingStatus { ACTIVE, CANCELLED_BY_CLIENT, CANCELLED_BY_STUDIO, COMPLETED, NO_SHOW }

enum class PaymentStatus { NOT_REQUIRED, PENDING, PAID, REFUNDED }

data class Rating(
  val id: UUID,
  val bookingId: UUID,
  val chefId: UUID,
  val clientId: UUID,
  val stars: Int,
  val comment: String?)
