package ru.cheftable.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class BookingLinkRepository(private val jdbc: JdbcTemplate) {
    fun findAllergenIdsByClientId(clientId: UUID): List<UUID> = jdbc.queryForList("select allergen_id from client_allergens where client_id = ? order by allergen_id", UUID::class.java, clientId)
    fun addRental(bookingId: UUID, rentalItemId: UUID, quantity: Int) = jdbc.update("insert into booking_rental_items (booking_id, rental_item_id, quantity) values (?, ?, ?) on conflict do nothing", bookingId, rentalItemId, quantity)
    fun addBookingAllergen(bookingId: UUID, allergenId: UUID) = jdbc.update("insert into booking_allergens (booking_id, allergen_id) values (?, ?) on conflict do nothing", bookingId, allergenId)
    fun addClientAllergen(clientId: UUID, allergenId: UUID) = jdbc.update("insert into client_allergens (client_id, allergen_id) values (?, ?) on conflict do nothing", clientId, allergenId)
}
