package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import java.util.UUID

interface PartnershipBillingRepository {
    fun getByPartnershipId(eventSlug: String, partnershipId: UUID): CompanyBillingData

    fun createOrUpdate(eventSlug: String, partnershipId: UUID, input: CompanyBillingData): UUID

    fun updateInvoiceUrl(eventSlug: String, partnershipId: UUID, invoiceUrl: String): UUID

    fun updateQuoteUrl(eventSlug: String, partnershipId: UUID, quoteUrl: String): UUID

    fun updateStatus(eventSlug: String, partnershipId: UUID, status: InvoiceStatus): UUID
}
