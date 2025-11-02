@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.sponsoring.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object SponsoringPacksTable : UUIDTable("sponsoring_packs") {
    val eventId = reference("event_id", EventsTable)
    val name = varchar("name", 255)
    val basePrice = integer("base_price")
    val withBooth = bool("with_booth").default(false)
    val nbTickets = integer("nb_ticket").default(0)
    val maxQuantity = integer("max_quantity").nullable()
}
