package ru.cheftable.persistence

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import java.time.OffsetDateTime
import java.util.UUID

interface ClientJpaRepository : JpaRepository<ClientEntity, UUID> { fun findByPhone(phone: String): ClientEntity? }
interface SlotJpaRepository : JpaRepository<SlotEntity, UUID> {
    @EntityGraph(attributePaths = ["program", "chef"])
    fun findByStartsAtBetweenOrderByStartsAtAsc(from: OffsetDateTime, to: OffsetDateTime): List<SlotEntity>
    @EntityGraph(attributePaths = ["program", "chef"])
    fun findWithDetailsById(id: UUID): SlotEntity?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = ["program", "chef"])
    fun findLockedWithDetailsById(id: UUID): SlotEntity?
}
interface BookingJpaRepository : JpaRepository<BookingEntity, UUID> {
    @EntityGraph(attributePaths = ["slot", "slot.program", "slot.chef"])
    @Query("select b from BookingEntity b where b.client.id = :clientId order by b.createdAt desc")
    fun findByClientIdOrderByCreatedAtDesc(@Param("clientId") clientId: UUID): List<BookingEntity>
    @EntityGraph(attributePaths = ["client", "slot", "slot.program", "slot.chef"])
    fun findWithDetailsById(id: UUID): BookingEntity?
}
interface RentalItemJpaRepository : JpaRepository<RentalItemEntity, UUID> { fun findByActiveTrueAndStockGreaterThanOrderByNameAsc(stock: Int = 0): List<RentalItemEntity> }
interface AllergenJpaRepository : JpaRepository<AllergenEntity, UUID> {
    fun findAllByOrderByNameAsc(): List<AllergenEntity>
    fun countByIdIn(ids: Collection<UUID>): Long
}
interface RatingJpaRepository : JpaRepository<RatingEntity, UUID> { fun findByBookingId(bookingId: UUID): RatingEntity? }

interface SmsChallengeJpaRepository : JpaRepository<SmsChallengeEntity, UUID> {
    fun findFirstByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phone: String): SmsChallengeEntity?
}
interface AuthSessionJpaRepository : JpaRepository<AuthSessionEntity, UUID> {
    fun findByTokenHashAndRevokedAtIsNull(tokenHash: String): AuthSessionEntity?
}
