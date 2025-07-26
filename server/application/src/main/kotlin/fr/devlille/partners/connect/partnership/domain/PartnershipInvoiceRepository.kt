package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.CompanyInvoice
import java.util.UUID

interface PartnershipInvoiceRepository {
    fun getByCompanyId(eventId: UUID, companyId: UUID): CompanyInvoice

    fun createOrUpdate(eventId: UUID, companyId: UUID, input: CompanyInvoice): UUID

    fun updateInvoiceUrl(eventId: UUID, companyId: UUID, invoiceUrl: String): UUID
}
