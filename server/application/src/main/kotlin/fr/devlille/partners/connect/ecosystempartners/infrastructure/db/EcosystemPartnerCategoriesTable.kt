package fr.devlille.partners.connect.ecosystempartners.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object EcosystemPartnerCategoriesTable : UUIDTable("event_ecosystem_partner_categories") {
    val eventId = reference("event_id", EventsTable)
    val name = text("name")
    val displayOrder = integer("display_order").nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        uniqueIndex("uk_ecosystem_partner_category_event_name", eventId, name)
        index(isUnique = false, eventId)
    }
}
