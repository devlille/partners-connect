package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.CompanyInvoice
import java.util.UUID

interface PartnershipInvoiceRepository {
    fun getByPartnershipId(eventId: UUID, partnershipId: UUID): CompanyInvoice

    fun createOrUpdate(eventId: UUID, partnershipId: UUID, input: CompanyInvoice): UUID

    fun updateInvoiceUrl(eventId: UUID, partnershipId: UUID, invoiceUrl: String): UUID
}
