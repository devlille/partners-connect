package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.CompanyInvoiceRepository
import fr.devlille.partners.connect.companies.domain.CompanyInvoice
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.companies.infrastructure.db.InvoiceEntity
import fr.devlille.partners.connect.companies.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.companies.infrastructure.db.InvoicesTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class CompanyInvoiceRepositoryExposed : CompanyInvoiceRepository {
    override fun getByCompanyId(companyId: UUID): CompanyInvoice = transaction {
        InvoiceEntity
            .find { InvoicesTable.companyId eq companyId }
            .singleOrNull()
            ?.let(InvoiceEntity::toDomain)
            ?: throw NotFoundException("Invoice not found for company ID: $companyId")
    }

    override fun createOrUpdate(companyId: UUID, input: CompanyInvoice): UUID = transaction {
        val existing = InvoiceEntity
            .find { InvoicesTable.companyId eq companyId }
            .singleOrNull()
        val company = CompanyEntity.findById(companyId) ?: throw NotFoundException("Company not found")
        if (existing == null) {
            InvoiceEntity.new {
                this.company = company
                this.name = input.name ?: company.name
                this.contactFirstName = input.contact.firstName
                this.contactSecondName = input.contact.lastName
                this.contactEmail = input.contact.email
                this.address = input.address.address
                this.city = input.address.city
                this.zipCode = input.address.zipCode
                this.country = input.address.country
                this.siret = input.siret
                this.vat = input.vat
                this.po = input.po
                this.status = InvoiceStatus.PENDING
            }
        } else {
            existing.apply {
                this.company = company
                this.name = input.name ?: company.name
                this.contactFirstName = input.contact.firstName
                this.contactSecondName = input.contact.lastName
                this.contactEmail = input.contact.email
                this.address = input.address.address
                this.city = input.address.city
                this.zipCode = input.address.zipCode
                this.country = input.address.country
                this.siret = input.siret
                this.vat = input.vat
                this.po = input.po ?: this.po
            }
        }.id.value
    }
}
