package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.billing.domain.Billing
import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.partnership.domain.PartnershipBillingRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipBillingRepositoryExposed : PartnershipBillingRepository {
    override fun getByPartnershipId(eventId: UUID, partnershipId: UUID): CompanyBillingData = transaction {
        BillingEntity
            .singleByEventAndPartnership(eventId, partnershipId)
            ?.let(BillingEntity::toDomain)
            ?: throw NotFoundException("Billing not found for partnership ID: $partnershipId")
    }

    override fun createOrUpdate(eventId: UUID, partnershipId: UUID, input: CompanyBillingData): UUID = transaction {
        val existing = BillingEntity.singleByEventAndPartnership(eventId, partnershipId)
        val partnership = PartnershipEntity.singleByEventAndPartnership(eventId, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        if (existing == null) {
            BillingEntity.new {
                this.event = partnership.event
                this.partnership = partnership
                this.name = input.name ?: partnership.company.name
                this.contactFirstName = input.contact.firstName
                this.contactLastName = input.contact.lastName
                this.contactEmail = input.contact.email
                this.po = input.po
                this.status = InvoiceStatus.PENDING
            }
        } else {
            existing.apply {
                this.name = input.name ?: partnership.company.name
                this.contactFirstName = input.contact.firstName
                this.contactLastName = input.contact.lastName
                this.contactEmail = input.contact.email
                this.po = input.po ?: this.po
            }
        }.id.value
    }

    override fun updateBillingUrls(eventId: UUID, partnershipId: UUID, billing: Billing): UUID = transaction {
        val existing = BillingEntity.singleByEventAndPartnership(eventId, partnershipId)
            ?: throw NotFoundException("Invoice not found for partnership ID: $partnershipId")
        existing.invoicePdfUrl = billing.invoiceUrl
        existing.quotePdfUrl = billing.quoteUrl
        existing.id.value
    }
}
