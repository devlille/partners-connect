package fr.devlille.partners.connect.provider.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class EventProviderEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EventProviderEntity>(EventProvidersTable)

    var eventId by EventProvidersTable.eventId
    var providerId by EventProvidersTable.providerId
}
