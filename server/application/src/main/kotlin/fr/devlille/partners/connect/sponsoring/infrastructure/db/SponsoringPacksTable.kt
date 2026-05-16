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
    val flyerTemplateUrl = text("flyer_template_url").nullable()
    val flyerZoneX = integer("flyer_zone_x").nullable()
    val flyerZoneY = integer("flyer_zone_y").nullable()
    val flyerZoneWidth = integer("flyer_zone_width").nullable()
    val flyerZoneHeight = integer("flyer_zone_height").nullable()
}
