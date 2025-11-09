package fr.devlille.partners.connect.provider.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class EventProviderEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EventProviderEntity>(EventProvidersTable)

    var event by EventEntity referencedOn EventProvidersTable.eventId
    var provider by ProviderEntity referencedOn EventProvidersTable.providerId
    var createdAt by EventProvidersTable.createdAt
}
