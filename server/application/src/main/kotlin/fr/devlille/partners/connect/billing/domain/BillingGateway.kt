package fr.devlille.partners.connect.billing.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

interface BillingGateway {
    val provider: IntegrationProvider

    suspend fun createInvoice(integrationId: UUID, eventId: UUID, partnershipId: UUID): String

    suspend fun createQuote(integrationId: UUID, eventId: UUID, partnershipId: UUID): String
}
