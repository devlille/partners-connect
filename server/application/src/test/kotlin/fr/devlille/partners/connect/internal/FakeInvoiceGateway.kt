package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.invoices.domain.InvoiceGateway
import java.util.UUID

class FakeInvoiceGateway : InvoiceGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.QONTO

    override fun createInvoice(
        integrationId: UUID,
        eventId: UUID,
        companyId: UUID,
    ): String = "https://fake-invoice-url.com/invoice/${UUID.randomUUID()}"
}
