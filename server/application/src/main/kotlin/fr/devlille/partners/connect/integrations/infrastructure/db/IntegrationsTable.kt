package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object IntegrationsTable : UUIDTable("integrations") {
    val eventId = uuid("event_id").references(EventsTable.id)
    val provider = enumeration<IntegrationProvider>(name = "provider")
    val usage = enumeration<IntegrationUsage>(name = "usage")
}
