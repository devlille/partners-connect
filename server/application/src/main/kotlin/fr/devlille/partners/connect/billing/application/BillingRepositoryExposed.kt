package fr.devlille.partners.connect.billing.application

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BillingRepositoryExposed(
    private val billingGateways: List<BillingGateway>,
) : BillingRepository {
    override suspend fun createInvoice(eventId: UUID, partnership: PartnershipDetail): String {
        val integration = transaction {
            IntegrationEntity.singleIntegration(eventId, IntegrationUsage.BILLING)
        }
        val gateway = billingGateways.find { it.provider == integration.provider }
            ?: throw NotFoundException("No gateway for provider ${integration.provider}")
        return gateway.createInvoice(integration.id.value, eventId, partnership)
    }

    override suspend fun createQuote(eventId: UUID, partnership: PartnershipDetail): String {
        val integration = transaction {
            IntegrationEntity.singleIntegration(eventId, IntegrationUsage.BILLING)
        }
        val gateway = billingGateways.find { it.provider == integration.provider }
            ?: throw NotFoundException("No gateway for provider ${integration.provider}")
        return gateway.createQuote(integration.id.value, eventId, partnership)
    }
}
