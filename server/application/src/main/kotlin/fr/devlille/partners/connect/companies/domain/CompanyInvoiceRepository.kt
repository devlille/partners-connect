package fr.devlille.partners.connect.companies.domain

import java.util.UUID

interface CompanyInvoiceRepository {
    fun getByCompanyId(companyId: UUID): CompanyInvoice
    fun createOrUpdate(companyId: UUID, input: CompanyInvoice): UUID
}
