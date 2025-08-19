package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.PartnershipBillingRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipBillingRepositoryExposed : PartnershipBillingRepository {
    override fun getByPartnershipId(eventSlug: String, partnershipId: UUID): CompanyBillingData = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        BillingEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?.let(BillingEntity::toDomain)
            ?: throw NotFoundException("Billing not found for partnership ID: $partnershipId")
    }

    override fun createOrUpdate(eventSlug: String, partnershipId: UUID, input: CompanyBillingData): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
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

    override fun updateInvoiceUrl(eventSlug: String, partnershipId: UUID, invoiceUrl: String): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val existing = BillingEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Invoice not found for partnership ID: $partnershipId")
        existing.invoicePdfUrl = invoiceUrl
        existing.id.value
    }

    override fun updateQuoteUrl(eventSlug: String, partnershipId: UUID, quoteUrl: String): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val existing = BillingEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Invoice not found for partnership ID: $partnershipId")
        existing.quotePdfUrl = quoteUrl
        existing.id.value
    }

    override fun updateStatus(eventSlug: String, partnershipId: UUID, status: InvoiceStatus): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val existing = BillingEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Billing not found")
        existing.status = status
        existing.id.value
    }
}
