package fr.devlille.partners.connect.integrations.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.Integration
import fr.devlille.partners.connect.integrations.domain.IntegrationRegistrar
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import fr.devlille.partners.connect.organisations.infrastructure.db.findBySlug as orgFindBySlug

class IntegrationRepositoryExposed(
    private val registrars: List<IntegrationRegistrar<*>>,
) : IntegrationRepository {
    override fun register(eventSlug: String, usage: IntegrationUsage, input: CreateIntegration): UUID {
        val eventId = transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            event.id.value
        }
        val registrar = registrars.find { it.supports(input) && usage in it.supportedUsages }
            ?: throw NotFoundException("No registrar found for input ${input::class.simpleName} and usage $usage")
        @Suppress("UNCHECKED_CAST")
        return (registrar as IntegrationRegistrar<CreateIntegration>)
            .register(eventId, usage, input)
    }

    override fun findByEvent(orgSlug: String, eventSlug: String): List<Integration> = transaction {
        // Verify organization exists
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")

        // Verify event exists and belongs to the organization
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        if (event.organisation.id != organisation.id) {
            throw NotFoundException("Event with slug $eventSlug not found in organisation $orgSlug")
        }

        // Query integrations for this event
        IntegrationsTable
            .selectAll()
            .where { IntegrationsTable.eventId eq event.id.value }
            .map { row ->
                Integration(
                    id = row[IntegrationsTable.id].value.toString(),
                    provider = row[IntegrationsTable.provider],
                    usage = row[IntegrationsTable.usage],
                    createdAt = row[IntegrationsTable.createdAt],
                    // For now, we don't expose sensitive integration details
                    details = emptyMap(),
                )
            }
    }

    override fun deleteById(orgSlug: String, eventSlug: String, integrationId: String): Boolean = transaction {
        // Verify organization exists
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")

        // Verify event exists and belongs to the organization
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        if (event.organisation.id != organisation.id) {
            throw NotFoundException("Event with slug $eventSlug not found in organisation $orgSlug")
        }

        // Parse the integration ID
        val integrationUuid = runCatching {
            UUID.fromString(integrationId)
        }.getOrElse {
            throw NotFoundException("Integration with id $integrationId not found")
        }

        // Verify integration exists and belongs to this event
        val integrationExists = IntegrationsTable
            .selectAll()
            .where {
                (IntegrationsTable.id eq integrationUuid) and
                    (IntegrationsTable.eventId eq event.id.value)
            }
            .singleOrNull() != null

        if (!integrationExists) {
            throw NotFoundException("Integration with id $integrationId not found")
        }

        // Delete the integration
        val deletedRows = IntegrationsTable.deleteWhere {
            (IntegrationsTable.id eq integrationUuid) and
                (IntegrationsTable.eventId eq event.id.value)
        }

        deletedRows > 0
    }
}
