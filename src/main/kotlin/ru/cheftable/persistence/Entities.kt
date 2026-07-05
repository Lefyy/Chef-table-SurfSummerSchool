package ru.cheftable.persistence

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "clients")
class ClientEntity(
  
  @Id
  var id: UUID? = null,
  
  @Column(nullable=false, unique=true)
  var phone: String = "",
  
  var createdAt: OffsetDateTime = OffsetDateTime.now()

)

@Entity
@Table(name = "chefs")
class ChefEntity(
  
  @Id
  var id: UUID? = null,
  
  @Column(nullable=false)
  var name: String = "",
  
  var bio: String? = null,
  
  var photoUrl: String? = null

)

@Entity
@Table(name = "programs")
class ProgramEntity(
  
  @Id
  var id: UUID? = null,
  
  var title: String = "",
  
  @Column(columnDefinition="text")
  var description: String = "",
  
  @Enumerated(EnumType.STRING)
  var difficulty: DifficultyLevelEntity = DifficultyLevelEntity.BEGINNER,
  
  var durationMinutes: Int = 0,
  
  var priceCents: Int = 0

)

enum class DifficultyLevelEntity { BEGINNER, INTERMEDIATE, ADVANCED }

@Entity
@Table(name = "slots")
class SlotEntity(
  
  @Id
  var id: UUID? = null,
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="program_id")
  var program: ProgramEntity? = null,
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="chef_id")
  var chef: ChefEntity? = null,
  
  var startsAt: OffsetDateTime = OffsetDateTime.now(),
  
  var endsAt: OffsetDateTime = OffsetDateTime.now(),
  
  var capacity: Int = 0,
  
  var bookedSeats: Int = 0,
  
  @Enumerated(EnumType.STRING)
  var status: SlotStatusEntity = SlotStatusEntity.SCHEDULED

)

enum class SlotStatusEntity { SCHEDULED, CANCELLED_BY_STUDIO }

@Entity
@Table(name = "rental_items")
class RentalItemEntity(
  
  @Id
  var id: UUID? = null,
  
  var name: String = "",
  
  var description: String? = null,
  
  var priceCents: Int = 0,
  
  var stock: Int = 0,
  
  var active: Boolean = true

)

@Entity
@Table(name = "allergens")
class AllergenEntity(
  
  @Id
  var id: UUID? = null,
  
  @Column(unique=true)
  var code: String = "",
  
  var name: String = ""

)

@Entity
@Table(name = "bookings")
class BookingEntity(
  
  @Id
  var id: UUID? = null,
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="client_id")
  var client: ClientEntity? = null,
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="slot_id")
  var slot: SlotEntity? = null,
  
  @Enumerated(EnumType.STRING)
  var status: BookingStatusEntity = BookingStatusEntity.ACTIVE,
  
  @Enumerated(EnumType.STRING)
  var paymentStatus: PaymentStatusEntity = PaymentStatusEntity.NOT_REQUIRED,
  
  var totalPriceCents: Int = 0,
  
  var attended: Boolean = false,
  
  var createdAt: OffsetDateTime = OffsetDateTime.now(),
  
  var cancelledAt: OffsetDateTime? = null

)

enum class BookingStatusEntity { ACTIVE, CANCELLED_BY_CLIENT, CANCELLED_BY_STUDIO, COMPLETED, NO_SHOW }

enum class PaymentStatusEntity { NOT_REQUIRED, PENDING, PAID, REFUNDED }

@Entity
@Table(name = "ratings")
class RatingEntity(
  
  @Id
  var id: UUID? = null,
  
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="booking_id")
  var booking: BookingEntity? = null,
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="chef_id")
  var chef: ChefEntity? = null,
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="client_id")
  var client: ClientEntity? = null,
  
  var stars: Int = 5,
  
  var comment: String? = null,
  
  var createdAt: OffsetDateTime = OffsetDateTime.now()

)
