package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.time.Clock

object IntegrationsTable : UUIDTable("integrations") {
    val eventId = javaUUID("event_id").references(EventsTable.id)
    val provider = enumeration<IntegrationProvider>(name = "provider")
    val usage = enumeration<IntegrationUsage>(name = "usage")
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
