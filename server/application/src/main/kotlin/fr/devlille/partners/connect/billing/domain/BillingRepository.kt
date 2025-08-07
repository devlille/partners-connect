package fr.devlille.partners.connect.billing.domain

import java.util.UUID

interface BillingRepository {
    suspend fun createInvoice(eventId: UUID, partnershipId: UUID): String

    suspend fun createQuote(eventId: UUID, partnershipId: UUID): String
}
