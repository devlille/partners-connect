package fr.devlille.partners.connect.billing.application

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
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
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )
        val eventId = event.id.value
        val integration = singleIntegration(eventId)
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = billingGateways.find { it.provider == provider }
            ?: throw NotFoundException(
                code = ErrorCode.PROVIDER_NOT_FOUND,
                message = "No gateway for provider $provider",
                meta = mapOf(MetaKeys.PROVIDER to provider.name),
            )
        gateway.createInvoice(integrationId, eventId, partnershipId)
    }

    override suspend fun createQuote(eventSlug: String, partnershipId: UUID): String = newSuspendedTransaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )
        val eventId = event.id.value
        val integration = singleIntegration(eventId)
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = billingGateways.find { it.provider == provider }
            ?: throw NotFoundException(
                code = ErrorCode.PROVIDER_NOT_FOUND,
                message = "No gateway for provider $provider",
                meta = mapOf(MetaKeys.PROVIDER to provider.name),
            )
        gateway.createQuote(integrationId, eventId, partnershipId)
    }

    private fun singleIntegration(eventId: UUID): ResultRow = transaction {
        val integrations = IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.BILLING)
            .toList()
        if (integrations.isEmpty()) {
            throw NotFoundException(
                code = ErrorCode.INTEGRATION_NOT_FOUND,
                message = "No billing integration found for event $eventId",
                meta = mapOf(MetaKeys.EVENT_ID to eventId.toString()),
            )
        }
        if (integrations.size > 1) {
            throw ConflictException(
                code = ErrorCode.MULTIPLE_INTEGRATIONS_FOUND,
                message = "Multiple billing integrations found for event $eventId",
                meta = mapOf(MetaKeys.EVENT_ID to eventId.toString()),
            )
        }
        integrations.single()
    }
}
