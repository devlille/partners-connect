package fr.devlille.partners.connect.billing.application

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipPricing
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class BillingRepositoryExposed(
    private val billingGateways: List<BillingGateway>,
) : BillingRepository {
    override suspend fun createInvoice(pricing: PartnershipPricing): String {
        val integration = transaction {
            IntegrationEntity.singleIntegration(pricing.eventId.toUUID(), IntegrationUsage.BILLING)
        }
        val gateway = billingGateways.find { it.provider == integration.provider }
            ?: throw NotFoundException("No gateway for provider ${integration.provider}")
        return gateway.createInvoice(integration.id.value, pricing)
    }

    override suspend fun createQuote(pricing: PartnershipPricing): String {
        val integration = transaction {
            IntegrationEntity.singleIntegration(pricing.eventId.toUUID(), IntegrationUsage.BILLING)
        }
        val gateway = billingGateways.find { it.provider == integration.provider }
            ?: throw NotFoundException("No gateway for provider ${integration.provider}")
        return gateway.createQuote(integration.id.value, pricing)
    }
}
