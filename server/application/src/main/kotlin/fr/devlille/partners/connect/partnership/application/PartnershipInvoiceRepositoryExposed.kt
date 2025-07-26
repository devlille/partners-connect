package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.CompanyInvoice
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.partnership.domain.PartnershipInvoiceRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoicesTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipInvoiceRepositoryExposed : PartnershipInvoiceRepository {
    override fun getByCompanyId(eventId: UUID, companyId: UUID): CompanyInvoice = transaction {
        InvoiceEntity.Companion
            .find { (InvoicesTable.eventId eq eventId) and (InvoicesTable.companyId eq companyId) }
            .singleOrNull()
            ?.let(InvoiceEntity::toDomain)
            ?: throw NotFoundException("Invoice not found for company ID: $companyId")
    }

    override fun createOrUpdate(eventId: UUID, companyId: UUID, input: CompanyInvoice): UUID = transaction {
        val existing = InvoiceEntity.Companion
            .find { (InvoicesTable.eventId eq eventId) and (InvoicesTable.companyId eq companyId) }
            .singleOrNull()
        val event = EventEntity.Companion.findById(eventId) ?: throw NotFoundException("Event not found")
        val company = CompanyEntity.Companion.findById(companyId) ?: throw NotFoundException("Company not found")
        if (existing == null) {
            InvoiceEntity.Companion.new {
                this.event = event
                this.company = company
                this.name = input.name ?: company.name
                this.contactFirstName = input.contact.firstName
                this.contactLastName = input.contact.lastName
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
                this.event = event
                this.company = company
                this.name = input.name ?: company.name
                this.contactFirstName = input.contact.firstName
                this.contactLastName = input.contact.lastName
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

    override fun updateInvoiceUrl(eventId: UUID, companyId: UUID, invoiceUrl: String): UUID = transaction {
        val existing = InvoiceEntity.Companion
            .find { (InvoicesTable.eventId eq eventId) and (InvoicesTable.companyId eq companyId) }
            .singleOrNull()
            ?: throw NotFoundException("Invoice not found for company ID: $companyId")
        existing.invoicePdfUrl = invoiceUrl
        existing.id.value
    }
}
