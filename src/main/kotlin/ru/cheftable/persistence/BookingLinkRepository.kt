package ru.cheftable.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.cheftable.application.booking.AllergenSelection
import ru.cheftable.application.booking.RentalSelection
import java.util.UUID

@Repository
class BookingLinkRepository(private val jdbc: JdbcTemplate) {
    fun findAllergenIdsByClientId(clientId: UUID): List<UUID> = jdbc.queryForList("select allergen_id from client_allergens where client_id = ? order by allergen_id", UUID::class.java, clientId)
    fun addRental(bookingId: UUID, rentalItemId: UUID, quantity: Int) = jdbc.update("insert into booking_rental_items (booking_id, rental_item_id, quantity) values (?, ?, ?) on conflict do nothing", bookingId, rentalItemId, quantity)
    fun addBookingAllergen(bookingId: UUID, allergenId: UUID) = jdbc.update("insert into booking_allergens (booking_id, allergen_id) values (?, ?) on conflict do nothing", bookingId, allergenId)
    fun addClientAllergen(clientId: UUID, allergenId: UUID) = jdbc.update("insert into client_allergens (client_id, allergen_id) values (?, ?) on conflict do nothing", clientId, allergenId)

    fun findClientAllergens(clientId: UUID): List<AllergenSelection> = jdbc.query("""
        select a.id, a.name from client_allergens ca join allergens a on a.id = ca.allergen_id
        where ca.client_id = ? order by a.name
    """.trimIndent(), { rs, _ -> AllergenSelection(rs.getObject("id", UUID::class.java), rs.getString("name")) }, clientId)

    fun findBookingAllergens(bookingId: UUID): List<AllergenSelection> = jdbc.query("""
        select a.id, a.name from booking_allergens ba join allergens a on a.id = ba.allergen_id
        where ba.booking_id = ? order by a.name
    """.trimIndent(), { rs, _ -> AllergenSelection(rs.getObject("id", UUID::class.java), rs.getString("name")) }, bookingId)

    fun findBookingRentals(bookingId: UUID): List<RentalSelection> = jdbc.query("""
        select r.id, r.name, r.price_cents, bri.quantity from booking_rental_items bri join rental_items r on r.id = bri.rental_item_id
        where bri.booking_id = ? order by r.name
    """.trimIndent(), { rs, _ -> RentalSelection(rs.getObject("id", UUID::class.java), rs.getString("name"), rs.getInt("price_cents"), rs.getInt("quantity")) }, bookingId)

    fun updateChefAverageRating(chefId: UUID) = jdbc.update("""
        update chefs set avg_rating = (select round(avg(stars)::numeric, 2) from ratings where chef_id = ?) where id = ?
    """.trimIndent(), chefId, chefId)
}
