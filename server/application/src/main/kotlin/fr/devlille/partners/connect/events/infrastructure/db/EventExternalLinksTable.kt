@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.events.infrastructure.db

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.time.Clock

object EventExternalLinksTable : UUIDTable("event_external_links") {
    val eventId = reference("event_id", EventsTable)
    val name = varchar("name", 255)
    val url = text("url")
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
