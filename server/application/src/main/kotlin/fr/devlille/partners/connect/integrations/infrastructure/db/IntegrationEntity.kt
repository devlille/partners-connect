package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class IntegrationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<IntegrationEntity>(IntegrationsTable)

    var eventId by IntegrationsTable.eventId
    var provider by IntegrationsTable.provider
    var usage by IntegrationsTable.usage
    var createdAt by IntegrationsTable.createdAt
    var event by EventEntity referencedOn IntegrationsTable.eventId
}
