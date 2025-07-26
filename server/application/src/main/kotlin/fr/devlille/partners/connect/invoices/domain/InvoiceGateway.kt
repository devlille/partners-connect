package fr.devlille.partners.connect.invoices.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

interface InvoiceGateway {
    val provider: IntegrationProvider

    fun createInvoice(integrationId: UUID, eventId: UUID, companyId: UUID): String
}
