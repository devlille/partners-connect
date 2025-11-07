package fr.devlille.partners.connect.billing.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.partnership.domain.PartnershipPricing
import java.util.UUID

interface BillingGateway {
    val provider: IntegrationProvider

    suspend fun createInvoice(integrationId: UUID, pricing: PartnershipPricing): String

    suspend fun createQuote(integrationId: UUID, pricing: PartnershipPricing): String
}
