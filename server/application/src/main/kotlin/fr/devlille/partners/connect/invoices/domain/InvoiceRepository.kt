package fr.devlille.partners.connect.invoices.domain

import java.util.UUID

interface InvoiceRepository {
    fun createInvoice(eventId: UUID, partnershipId: UUID): String
}
