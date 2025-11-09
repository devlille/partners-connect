package fr.devlille.partners.connect.provider.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Database table definition for event-provider relationship mappings.
 *
 * This join table manages many-to-many relationships between events and providers
 * within the same organisation context. Providers can be attached to multiple events,
 * and events can have multiple providers attached for partnership management.
 *
 * Database constraints:
 * - eventId: foreign key to EventsTable (non-null)
 * - providerId: foreign key to ProvidersTable (non-null)
 * - createdAt: auto-populated timestamp when attachment was created
 * - Unique constraint on (eventId, providerId) to prevent duplicates
 *
 * Business rules enforced at application level:
 * - Events and providers must belong to the same organisation
 * - Only organisation members with edit permissions can manage attachments
 */
object EventProvidersTable : UUIDTable("event_providers") {
    /** Foreign key reference to the event (required) */
    val eventId = reference("event_id", EventsTable)

    /** Foreign key reference to the provider (required) */
    val providerId = reference("provider_id", ProvidersTable)

    /** Timestamp when the provider was attached to the event (auto-populated) */
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        // Create unique index to prevent duplicate event-provider attachments
        uniqueIndex(eventId, providerId)
    }
}
