package ru.cheftable.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime

@DataJpaTest
@ActiveProfiles("test")
class PersistenceSmokeTest @Autowired constructor(
    private val slots: SlotJpaRepository,
    private val allergens: AllergenJpaRepository,
    private val rentals: RentalItemJpaRepository,
) {
    @Test
    fun `flyway seed data is queryable through repositories`() {
        assertThat(allergens.findAllByOrderByNameAsc()).extracting<String> { it.code }.contains("gluten", "nuts")
        assertThat(rentals.findByActiveTrueAndStockGreaterThanOrderByNameAsc()).isNotEmpty
        assertThat(slots.findByStartsAtBetweenOrderByStartsAtAsc(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(7))).hasSize(1)
    }
}
