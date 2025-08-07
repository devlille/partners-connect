package fr.devlille.partners.connect.billing.domain

import java.util.UUID

interface BillingRepository {
    fun createBilling(eventId: UUID, partnershipId: UUID): Billing
}
