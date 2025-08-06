package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.CompanyInvoice
import fr.devlille.partners.connect.partnership.domain.PartnershipInvoiceRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipInvoiceRepositoryExposed : PartnershipInvoiceRepository {
    override fun getByPartnershipId(eventId: UUID, partnershipId: UUID): CompanyInvoice = transaction {
        InvoiceEntity
            .singleByEventAndPartnership(eventId, partnershipId)
            ?.let(InvoiceEntity::toDomain)
            ?: throw NotFoundException("Invoice not found for partnership ID: $partnershipId")
    }

    override fun createOrUpdate(eventId: UUID, partnershipId: UUID, input: CompanyInvoice): UUID = transaction {
        val existing = InvoiceEntity.singleByEventAndPartnership(eventId, partnershipId)
        val partnership = PartnershipEntity.singleByEventAndPartnership(eventId, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        if (existing == null) {
            InvoiceEntity.new {
                this.event = partnership.event
                this.partnership = partnership
                this.name = input.name ?: partnership.company.name
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
                this.name = input.name ?: partnership.company.name
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

    override fun updateInvoiceUrl(eventId: UUID, partnershipId: UUID, invoiceUrl: String): UUID = transaction {
        val existing = InvoiceEntity.singleByEventAndPartnership(eventId, partnershipId)
            ?: throw NotFoundException("Invoice not found for partnership ID: $partnershipId")
        existing.invoicePdfUrl = invoiceUrl
        existing.id.value
    }
}
