package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class IntegrationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<IntegrationEntity>(IntegrationsTable) {
        fun singleIntegration(eventId: UUID, usage: IntegrationUsage): IntegrationEntity {
            val integrations = this
                .find { IntegrationsTable.eventId eq eventId and (IntegrationsTable.usage eq usage) }
                .toList()
            if (integrations.isEmpty()) {
                throw NotFoundException("No $usage integration found for event $eventId")
            }
            if (integrations.size > 1) {
                throw NotFoundException("Multiple $usage integrations found for event $eventId")
            }
            return integrations.single()
        }
    }

    var eventId by IntegrationsTable.eventId
    var provider by IntegrationsTable.provider
    var usage by IntegrationsTable.usage
    var createdAt by IntegrationsTable.createdAt
    var event by EventEntity referencedOn IntegrationsTable.eventId
}
