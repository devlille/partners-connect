@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.sponsoring.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object SponsoringPacksTable : UUIDTable("sponsoring_packs") {
    val eventId = uuid("event_id").references(EventsTable.id)
    val name = varchar("name", 255)
    val basePrice = integer("base_price")
    val maxQuantity = integer("max_quantity").nullable()
}
