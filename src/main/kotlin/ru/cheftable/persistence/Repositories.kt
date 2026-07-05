package ru.cheftable.persistence

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface ClientJpaRepository : JpaRepository<ClientEntity, UUID> { fun findByPhone(phone: String): ClientEntity? }
interface SlotJpaRepository : JpaRepository<SlotEntity, UUID> {
    @EntityGraph(attributePaths = ["program", "chef"])
    fun findByStartsAtBetweenOrderByStartsAtAsc(from: OffsetDateTime, to: OffsetDateTime): List<SlotEntity>
    @EntityGraph(attributePaths = ["program", "chef"])
    fun findWithDetailsById(id: UUID): SlotEntity?
}
interface BookingJpaRepository : JpaRepository<BookingEntity, UUID> {
    @EntityGraph(attributePaths = ["slot", "slot.program", "slot.chef"])
    fun findByClientIdOrderByCreatedAtDesc(clientId: UUID): List<BookingEntity>
    @EntityGraph(attributePaths = ["client", "slot", "slot.program", "slot.chef"])
    fun findWithDetailsById(id: UUID): BookingEntity?
}
interface RentalItemJpaRepository : JpaRepository<RentalItemEntity, UUID> { fun findByActiveTrueAndStockGreaterThanOrderByNameAsc(stock: Int = 0): List<RentalItemEntity> }
interface AllergenJpaRepository : JpaRepository<AllergenEntity, UUID> { fun findAllByOrderByNameAsc(): List<AllergenEntity> }
interface RatingJpaRepository : JpaRepository<RatingEntity, UUID> { fun findByBookingId(bookingId: UUID): RatingEntity? }

interface SmsChallengeJpaRepository : JpaRepository<SmsChallengeEntity, UUID> {
    fun findFirstByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phone: String): SmsChallengeEntity?
}
interface AuthSessionJpaRepository : JpaRepository<AuthSessionEntity, UUID> {
    fun findByTokenHashAndRevokedAtIsNull(tokenHash: String): AuthSessionEntity?
}
