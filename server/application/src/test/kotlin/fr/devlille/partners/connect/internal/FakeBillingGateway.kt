package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.billing.domain.Billing
import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

class FakeBillingGateway : BillingGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.QONTO

    override fun createBilling(
        integrationId: UUID,
        eventId: UUID,
        partnershipId: UUID,
    ): Billing = Billing(
        invoiceUrl = "https://fake-invoice-url.com/invoice/${UUID.randomUUID()}",
        quoteUrl = "https://fake-quote-url.com/quote/${UUID.randomUUID()}",
    )
}
