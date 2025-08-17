package fr.devlille.partners.connect.billing.application

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BillingRepositoryExposed(
    private val billingGateways: List<BillingGateway>,
) : BillingRepository {
    override suspend fun createInvoice(eventSlug: String, partnershipId: UUID): String = newSuspendedTransaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val integration = singleIntegration(eventId)
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = billingGateways.find { it.provider == provider }
            ?: throw NotFoundException("No gateway for provider $provider")
        gateway.createInvoice(integrationId, eventId, partnershipId)
    }

    override suspend fun createQuote(eventSlug: String, partnershipId: UUID): String = newSuspendedTransaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val integration = singleIntegration(eventId)
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = billingGateways.find { it.provider == provider }
            ?: throw NotFoundException("No gateway for provider $provider")
        gateway.createQuote(integrationId, eventId, partnershipId)
    }

    private fun singleIntegration(eventId: UUID): ResultRow = transaction {
        val integrations = IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.BILLING)
            .toList()
        if (integrations.isEmpty()) {
            throw NotFoundException("No billing integration found for event $eventId")
        }
        if (integrations.size > 1) {
            throw NotFoundException("Multiple billing integrations found for event $eventId")
        }
        integrations.single()
    }
}
