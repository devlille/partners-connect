package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import java.util.UUID

class FakeBillingGateway : BillingGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.QONTO

    override suspend fun createInvoice(
        integrationId: UUID,
        eventId: UUID,
        partnership: PartnershipDetail,
    ): String = "https://fake-invoice-url.com/invoice/${UUID.randomUUID()}"

    override suspend fun createQuote(
        integrationId: UUID,
        eventId: UUID,
        partnership: PartnershipDetail,
    ): String = "https://fake-quote-url.com/quote/${UUID.randomUUID()}"
}
