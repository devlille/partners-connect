package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.PartnershipTicketRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketsTable
import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

class PartnershipTicketRepositoryExposed : PartnershipTicketRepository {
    override fun create(
        eventSlug: String,
        partnershipId: UUID,
        order: TicketOrder,
    ): List<String> = transaction {
        val event = EventEntity.findBySlug(eventSlug) ?: throw NotFoundException("Event $eventSlug not found")
        val billing = BillingEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Billing entity not found")
        order.tickets.forEach { ticket ->
            PartnershipTicketEntity.new(ticket.id) {
                this.partnership = billing.partnership
                this.orderId = order.id
                this.externalId = ticket.extId
                this.url = ticket.url
                this.firstname = ticket.data.firstName
                this.lastname = ticket.data.lastName
                this.email = billing.contactEmail
            }
        }
        order.tickets.map { it.id }
    }

    override fun update(
        ticket: Ticket,
        input: TicketData,
    ): String = transaction {
        PartnershipTicketsTable.upsert(PartnershipTicketsTable.id) {
            it[PartnershipTicketsTable.id] = ticket.id
            it[PartnershipTicketsTable.externalId] = ticket.extId
            it[PartnershipTicketsTable.url] = ticket.url
            it[PartnershipTicketsTable.firstname] = input.firstName
            it[PartnershipTicketsTable.lastname] = input.lastName
        }
        ticket.id
    }
}
