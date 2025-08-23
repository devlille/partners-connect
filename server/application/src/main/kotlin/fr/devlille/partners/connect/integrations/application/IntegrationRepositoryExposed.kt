package fr.devlille.partners.connect.integrations.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.Integration
import fr.devlille.partners.connect.integrations.domain.IntegrationRegistrar
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
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
                )
            }
    }

    override fun deleteById(
        orgSlug: String,
        eventSlug: String,
        usage: IntegrationUsage,
        integrationId: UUID,
    ) = transaction {
        // Verify organization exists
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")

        // Verify event exists and belongs to the organization
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        if (event.organisation.id != organisation.id) {
            throw NotFoundException("Event with slug $eventSlug not found in organisation $orgSlug")
        }

        // Find and delete the integration if it exists and belongs to this event
        val integrationEntity = IntegrationEntity.find {
            (IntegrationsTable.id eq integrationId) and
                (IntegrationsTable.eventId eq event.id.value)
        }.firstOrNull()

        if (integrationEntity == null) {
            throw NotFoundException("Integration with id $integrationId not found")
        }

        val registrars = registrars.filter { usage in it.supportedUsages }
        for (registrar in registrars) {
            try {
                @Suppress("UNCHECKED_CAST")
                (registrar as IntegrationRegistrar<CreateIntegration>).unregister(integrationId)
            } catch (_: Exception) {
                // Log and continue to the next registrar
            }
        }
    }
}
