package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import java.util.UUID

interface PartnershipBillingRepository {
    fun getByPartnershipId(eventId: UUID, partnershipId: UUID): CompanyBillingData

    fun createOrUpdate(eventId: UUID, partnershipId: UUID, input: CompanyBillingData): UUID

    fun updateInvoiceUrl(eventId: UUID, partnershipId: UUID, invoiceUrl: String): UUID

    fun updateQuoteUrl(eventId: UUID, partnershipId: UUID, quoteUrl: String): UUID

    fun updateStatus(eventId: UUID, partnershipId: UUID, status: InvoiceStatus): UUID
}
