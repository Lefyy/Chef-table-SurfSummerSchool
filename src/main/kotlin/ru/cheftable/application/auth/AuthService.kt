package ru.cheftable.application.auth

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import ru.cheftable.infrastructure.logging.maskPhone
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cheftable.persistence.*
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

@Service
class AuthService(
    private val smsChallenges: SmsChallengeJpaRepository,
    private val sessions: AuthSessionJpaRepository,
    private val clients: ClientJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${chef-table.auth.dev-code:1234}") private val devCode: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun requestSms(phone: String): SmsRequestResult {
        val now = OffsetDateTime.now()
        smsChallenges.save(SmsChallengeEntity(UUID.randomUUID(), normalize(phone), passwordEncoder.encode(devCode), now.plusMinutes(5), null, now))
        log.info("sms_requested event_type=sms_requested phone_mask={}", maskPhone(phone))
        return SmsRequestResult(300, 60, 3, 5)
    }

    @Transactional
    fun verifySms(phone: String, code: String): AuthResult {
        val normalizedPhone = normalize(phone)
        val challenge = smsChallenges.findFirstByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(normalizedPhone)
            ?: throw UnauthorizedException("SMS code was not requested")
        if (challenge.expiresAt.isBefore(OffsetDateTime.now()) || !passwordEncoder.matches(code, challenge.codeHash)) {
            throw UnauthorizedException("Invalid SMS code")
        }
        challenge.consumedAt = OffsetDateTime.now()
        val client = clients.findByPhone(normalizedPhone) ?: clients.save(ClientEntity(UUID.randomUUID(), normalizedPhone, OffsetDateTime.now()))
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().toByteArray())
        sessions.save(AuthSessionEntity(UUID.randomUUID(), client, hash(token), OffsetDateTime.now().plusDays(30), null, OffsetDateTime.now()))
        log.info("auth_success event_type=auth_success client_id={} phone_mask={}", client.id, maskPhone(client.phone))
        return AuthResult(token, client.id!!, client.phone)
    }

    @Transactional(readOnly = true)
    fun authenticate(token: String): AuthenticatedClient? = sessions.findByTokenHashAndRevokedAtIsNull(hash(token))
        ?.takeIf { it.expiresAt.isAfter(OffsetDateTime.now()) }
        ?.let { AuthenticatedClient(it.client!!.id!!, it.client!!.phone) }

    @Transactional
    fun logout(token: String) { sessions.findByTokenHashAndRevokedAtIsNull(hash(token))?.revokedAt = OffsetDateTime.now() }

    private fun normalize(phone: String) = phone.trim().replace(" ", "")
    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

data class SmsRequestResult(val expiresInSeconds: Int, val resendAvailableInSeconds: Int, val maxVerificationAttempts: Int, val maxResendAttempts: Int)
data class AuthResult(val accessToken: String, val clientId: UUID, val phone: String)
data class AuthenticatedClient(val id: UUID, val phone: String)
class UnauthorizedException(message: String) : RuntimeException(message)
