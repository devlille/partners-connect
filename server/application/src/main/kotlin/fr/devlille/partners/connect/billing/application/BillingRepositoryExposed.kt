package fr.devlille.partners.connect.billing.application

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BillingRepositoryExposed(
    private val billingGateways: List<BillingGateway>,
) : BillingRepository {
    override suspend fun createInvoice(eventSlug: String, partnershipId: UUID): String = newSuspendedTransaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.ENTITY_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
            )
        val eventId = event.id.value
        val integration = singleIntegration(eventId)
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = billingGateways.find { it.provider == provider }
            ?: throw NotFoundException(
                code = ErrorCode.ENTITY_NOT_FOUND,
                message = "No gateway for provider $provider",
            )
        gateway.createInvoice(integrationId, eventId, partnershipId)
    }

    override suspend fun createQuote(eventSlug: String, partnershipId: UUID): String = newSuspendedTransaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.ENTITY_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
            )
        val eventId = event.id.value
        val integration = singleIntegration(eventId)
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = billingGateways.find { it.provider == provider }
            ?: throw NotFoundException(
                code = ErrorCode.ENTITY_NOT_FOUND,
                message = "No gateway for provider $provider",
            )
        gateway.createQuote(integrationId, eventId, partnershipId)
    }

    private fun singleIntegration(eventId: UUID): ResultRow = transaction {
        val integrations = IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.BILLING)
            .toList()
        if (integrations.isEmpty()) {
            throw NotFoundException(
                code = ErrorCode.ENTITY_NOT_FOUND,
                message = "No billing integration found for event $eventId",
            )
        }
        if (integrations.size > 1) {
            throw NotFoundException(
                code = ErrorCode.ENTITY_NOT_FOUND,
                message = "Multiple billing integrations found for event $eventId",
            )
        }
        integrations.single()
    }
}
