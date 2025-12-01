package fr.devlille.partners.connect.billing.domain

import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import java.util.UUID

interface BillingRepository {
    suspend fun createInvoice(eventId: UUID, partnership: PartnershipDetail): String

    suspend fun createQuote(eventId: UUID, partnership: PartnershipDetail): String
}
