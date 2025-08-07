package fr.devlille.partners.connect.billing.application

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BillingRepositoryExposed(
    private val billingGateways: List<BillingGateway>,
) : BillingRepository {
    override fun createBilling(eventId: UUID, partnershipId: UUID): String = transaction {
        val integrations = IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.BILLING)
            .toList()
        if (integrations.isEmpty()) {
            throw NotFoundException("No billing integration found for event $eventId")
        }
        if (integrations.size > 1) {
            throw NotFoundException("Multiple billing integrations found for event $eventId")
        }
        val integration = integrations.single()
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = billingGateways.find { it.provider == provider }
            ?: throw NotFoundException("No gateway for provider $provider")
        gateway.createBilling(integrationId, eventId, partnershipId)
    }
}
