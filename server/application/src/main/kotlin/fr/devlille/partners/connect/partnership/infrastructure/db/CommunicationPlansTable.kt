@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object CommunicationPlansTable : UUIDTable("communication_plans") {
    val eventId = reference("event_id", EventsTable)
    val partnershipId = reference(
        "partnership_id",
        PartnershipsTable,
        onDelete = ReferenceOption.SET_NULL,
    ).nullable()
    val title = varchar("title", 255)
    val scheduledDate = datetime("scheduled_date").nullable()
    val description = text("description").nullable()
    val supportUrl = text("support_url").nullable()
    val createdAt = datetime("created_at")
        .clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
    val updatedAt = datetime("updated_at")
        .clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }

    init {
        index(false, eventId)
    }
}
