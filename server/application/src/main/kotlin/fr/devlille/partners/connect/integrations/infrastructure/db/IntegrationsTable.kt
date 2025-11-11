package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

object IntegrationsTable : UUIDTable("integrations") {
    val eventId = uuid("event_id").references(EventsTable.id)
    val provider = enumeration<IntegrationProvider>(name = "provider")
    val usage = enumeration<IntegrationUsage>(name = "usage")
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}

fun IntegrationsTable.findByEventIdAndUsage(eventId: UUID, usage: IntegrationUsage) = this
    .selectAll()
    .where { (IntegrationsTable.eventId eq eventId) and (IntegrationsTable.usage eq usage) }

fun IntegrationsTable.singleIntegration(eventId: UUID, usage: IntegrationUsage): Pair<IntegrationProvider, UUID> {
    val integrations = findByEventIdAndUsage(eventId, usage).toList()
    if (integrations.isEmpty()) {
        throw NotFoundException("No $usage integration found for event $eventId")
    }
    if (integrations.size > 1) {
        throw NotFoundException("Multiple $usage integrations found for event $eventId")
    }
    val integration = integrations.single()
    val provider = integration[IntegrationsTable.provider]
    val integrationId = integration[IntegrationsTable.id].value
    return Pair(provider, integrationId)
}
