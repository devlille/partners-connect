package fr.devlille.partners.connect.billing.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

interface BillingGateway {
    val provider: IntegrationProvider

    fun createBilling(integrationId: UUID, eventId: UUID, partnershipId: UUID): Billing
}
