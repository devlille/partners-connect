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
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
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
                ?: throw NotFoundException(
                    code = ErrorCode.EVENT_NOT_FOUND,
                    message = "Event with slug $eventSlug not found",
                    meta = mapOf(MetaKeys.EVENT to eventSlug),
                )
            event.id.value
        }
        val registrar = registrars.find { it.supports(input) && usage in it.supportedUsages }
            ?: throw NotFoundException(
                code = ErrorCode.INTEGRATION_NOT_FOUND,
                message = "No registrar found for input ${input::class.simpleName} and usage $usage",
                meta = mapOf(
                    MetaKeys.RESOURCE to "registrar",
                    MetaKeys.OPERATION to usage.toString(),
                ),
            )
        @Suppress("UNCHECKED_CAST")
        return (registrar as IntegrationRegistrar<CreateIntegration>)
            .register(eventId, usage, input)
    }

    override fun findByEvent(orgSlug: String, eventSlug: String): List<Integration> = transaction {
        // Verify organization exists
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException(
                code = ErrorCode.ORGANISATION_NOT_FOUND,
                message = "Organisation with slug $orgSlug not found",
                meta = mapOf(MetaKeys.ORGANISATION to orgSlug),
            )

        // Verify event exists and belongs to the organization
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )

        if (event.organisation.id != organisation.id) {
            throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found in organisation $orgSlug",
                meta = mapOf(
                    MetaKeys.EVENT to eventSlug,
                    MetaKeys.ORGANISATION to orgSlug,
                ),
            )
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

    override fun deleteById(orgSlug: String, eventSlug: String, integrationId: UUID) = transaction {
        // Verify organization exists
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException(
                code = ErrorCode.ORGANISATION_NOT_FOUND,
                message = "Organisation with slug $orgSlug not found",
                meta = mapOf(MetaKeys.ORGANISATION to orgSlug),
            )

        // Verify event exists and belongs to the organization
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )

        if (event.organisation.id != organisation.id) {
            throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found in organisation $orgSlug",
                meta = mapOf(
                    MetaKeys.EVENT to eventSlug,
                    MetaKeys.ORGANISATION to orgSlug,
                ),
            )
        }

        // Find and delete the integration if it exists and belongs to this event
        val integrationEntity = IntegrationEntity.find {
            (IntegrationsTable.id eq integrationId) and
                (IntegrationsTable.eventId eq event.id.value)
        }.firstOrNull()

        if (integrationEntity == null) {
            throw NotFoundException(
                code = ErrorCode.INTEGRATION_NOT_FOUND,
                message = "Integration with id $integrationId not found",
                meta = mapOf(MetaKeys.ID to integrationId.toString()),
            )
        }

        integrationEntity.delete()
    }
}
