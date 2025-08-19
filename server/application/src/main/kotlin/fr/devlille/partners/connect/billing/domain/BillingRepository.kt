package fr.devlille.partners.connect.billing.domain

import java.util.UUID

interface BillingRepository {
    suspend fun createInvoice(eventSlug: String, partnershipId: UUID): String

    suspend fun createQuote(eventSlug: String, partnershipId: UUID): String
}
