package fr.devlille.partners.connect.tickets.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.listByPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketGateway
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import fr.devlille.partners.connect.tickets.domain.TicketRepository
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

class TicketRepositoryExposed(
    private val gateways: List<TicketGateway>,
) : TicketRepository {
    override fun listTickets(partnershipId: UUID): List<Ticket> = transaction {
        val tickets = PartnershipTicketEntity.listByPartnership(partnershipId)
        if (tickets.isEmpty()) {
            throw NotFoundException("No tickets found for partnership $partnershipId")
        }
        tickets.map { ticket ->
            Ticket(
                id = ticket.id.value,
                extId = ticket.externalId,
                url = ticket.url,
                data = TicketData(
                    firstName = ticket.firstname,
                    lastName = ticket.lastname,
                ),
            )
        }
    }

    override suspend fun createTickets(eventSlug: String, partnershipId: UUID, tickets: List<TicketData>): TicketOrder {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val integration = singleIntegration(eventId)
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = gateways.find { it.provider == provider }
            ?: throw NotFoundException("No gateway for provider $provider")
        val billing = transaction { BillingEntity.singleByEventAndPartnership(eventId, partnershipId) }
            ?: throw NotFoundException("Billing entity not found for event $eventId and partnership $partnershipId")
        if (billing.status != InvoiceStatus.PAID) {
            throw ForbiddenException("Invoice status ${billing.status} is not PAID")
        }
        val validatedPack = transaction { billing.partnership.validatedPack() }
        val partnership = transaction { billing.partnership }
        if (validatedPack == null) {
            throw NotFoundException("No validated pack found for partnership ${partnership.id}")
        } else if (validatedPack.nbTickets < tickets.size) {
            val message = """
Not enough tickets in the validated pack: ${validatedPack.nbTickets} available, ${tickets.size} requested
            """.trimIndent()
            throw ForbiddenException(message)
        }
        val order = gateway.createTickets(integrationId, eventId, partnershipId, tickets)
        transaction {
            order.tickets.forEach { ticket ->
                PartnershipTicketEntity.new(ticket.id) {
                    this.partnership = partnership
                    this.orderId = order.id
                    this.externalId = ticket.extId
                    this.url = ticket.url
                    this.firstname = ticket.data.firstName
                    this.lastname = ticket.data.lastName
                    this.email = billing.contactEmail
                }
            }
        }
        return order
    }

    override suspend fun updateTicket(eventSlug: String, partnershipId: UUID, ticketId: String, data: TicketData): Ticket {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val integration = singleIntegration(eventId)
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = gateways.find { it.provider == provider }
            ?: throw NotFoundException("No gateway for provider $provider")
        val ticket = gateway.updateTicket(integrationId, ticketId, data)
        transaction {
            PartnershipTicketsTable.upsert(PartnershipTicketsTable.id) {
                it[PartnershipTicketsTable.id] = ticket.id
                it[PartnershipTicketsTable.externalId] = ticket.extId
                it[PartnershipTicketsTable.url] = ticket.url
                it[PartnershipTicketsTable.firstname] = data.firstName
                it[PartnershipTicketsTable.lastname] = data.lastName
            }
        }
        return ticket
    }

    private fun singleIntegration(eventId: UUID): ResultRow = transaction {
        val integrations = IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.TICKETING)
            .toList()
        if (integrations.isEmpty()) {
            throw NotFoundException("No ticketing integration found for event $eventId")
        }
        if (integrations.size > 1) {
            throw NotFoundException("Multiple ticketing integrations found for event $eventId")
        }
        integrations.single()
    }
}
