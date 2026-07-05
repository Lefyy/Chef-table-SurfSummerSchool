package ru.cheftable.application.booking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import ru.cheftable.application.auth.AuthenticatedClient
import ru.cheftable.persistence.BookingLinkRepository
import ru.cheftable.persistence.ClientEntity
import ru.cheftable.persistence.ClientJpaRepository
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class BookingCreationServiceTest @Autowired constructor(
    private val bookingCreation: BookingCreationService,
    private val clients: ClientJpaRepository,
    private val bookingLinks: BookingLinkRepository,
) {
    @Test
    fun `create flushes booking before inserting rental links`() {
        val clientId = UUID.randomUUID()
        clients.saveAndFlush(ClientEntity(clientId, "+79990000000", OffsetDateTime.now()))

        val booking = bookingCreation.create(
            AuthenticatedClient(clientId, "+79990000000"),
            CreateBookingCommand(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                listOf(UUID.fromString("40000000-0000-0000-0000-000000000001")),
                emptyList(),
            ),
        )

        assertThat(bookingLinks.findBookingRentals(requireNotNull(booking.id)).map { it.id })
            .containsExactly(UUID.fromString("40000000-0000-0000-0000-000000000001"))
    }
}
