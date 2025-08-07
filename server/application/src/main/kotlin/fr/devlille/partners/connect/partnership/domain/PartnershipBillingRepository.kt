package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import java.util.UUID

interface PartnershipBillingRepository {
    fun getByPartnershipId(eventId: UUID, partnershipId: UUID): CompanyBillingData

    fun createOrUpdate(eventId: UUID, partnershipId: UUID, input: CompanyBillingData): UUID

    fun updateInvoiceUrl(eventId: UUID, partnershipId: UUID, invoiceUrl: String): UUID
}
