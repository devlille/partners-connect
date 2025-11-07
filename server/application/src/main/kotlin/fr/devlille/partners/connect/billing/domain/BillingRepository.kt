package fr.devlille.partners.connect.billing.domain

import fr.devlille.partners.connect.partnership.domain.PartnershipPricing

interface BillingRepository {
    suspend fun createInvoice(pricing: PartnershipPricing): String

    suspend fun createQuote(pricing: PartnershipPricing): String
}
