package fr.devlille.partners.connect.provider.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object EventProvidersTable : UUIDTable("event_providers") {
    val eventId = reference("event_id", EventsTable)
    val providerId = reference("provider_id", ProvidersTable)

    init {
        // Create unique index to prevent duplicate entries
        uniqueIndex(eventId, providerId)
    }
}
