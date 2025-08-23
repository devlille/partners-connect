@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.events.infrastructure.db

import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object EventWebhooksTable : UUIDTable("event_webhooks") {
    val eventId = reference("event_id", EventsTable)
    val url: Column<String> = text("url")
    val type: Column<String> = varchar("type", 50) // 'all' or 'partnership'
    val partnershipId = reference("partnership_id", PartnershipsTable).nullable()
    val headerAuth: Column<String?> = text("header_auth").nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    val updatedAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
