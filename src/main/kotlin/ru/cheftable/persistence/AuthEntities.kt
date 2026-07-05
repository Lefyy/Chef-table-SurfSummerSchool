package ru.cheftable.persistence

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "sms_challenges")
class SmsChallengeEntity(
    @Id var id: UUID? = null,
    @Column(nullable = false) var phone: String = "",
    @Column(nullable = false) var codeHash: String = "",
    @Column(nullable = false) var expiresAt: OffsetDateTime = OffsetDateTime.now(),
    var consumedAt: OffsetDateTime? = null,
    @Column(nullable = false) var createdAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "auth_sessions")
class AuthSessionEntity(
    @Id var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id") var client: ClientEntity? = null,
    @Column(nullable = false, unique = true) var tokenHash: String = "",
    @Column(nullable = false) var expiresAt: OffsetDateTime = OffsetDateTime.now(),
    var revokedAt: OffsetDateTime? = null,
    @Column(nullable = false) var createdAt: OffsetDateTime = OffsetDateTime.now(),
)
