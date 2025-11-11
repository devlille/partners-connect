package fr.devlille.partners.connect.agenda.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object SessionsTable : UUIDTable("sessions") {
    val externalId = varchar("externalId", length = 255).uniqueIndex()
    val name = varchar("name", length = 255)
    val abstract = text("abstract").nullable()
    val startTime = datetime("start_time").nullable()
    val endTime = datetime("end_time").nullable()
    val trackName = varchar("track_name", length = 255).nullable()
    val language = varchar("language", length = 50).nullable()
    val eventId = reference("event_id", EventsTable)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
