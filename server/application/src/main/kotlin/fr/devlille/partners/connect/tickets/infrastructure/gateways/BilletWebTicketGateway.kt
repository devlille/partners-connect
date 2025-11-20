package fr.devlille.partners.connect.tickets.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketGateway
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import fr.devlille.partners.connect.tickets.infrastructure.gateways.mappers.toCreateOrderProduct
import fr.devlille.partners.connect.tickets.infrastructure.gateways.mappers.toDomain
import fr.devlille.partners.connect.tickets.infrastructure.gateways.mappers.toOrderRequest
import fr.devlille.partners.connect.tickets.infrastructure.providers.BilletWebProvider
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BilletWebTicketGateway(
    private val billetWebProvider: BilletWebProvider,
) : TicketGateway {
    override val provider: IntegrationProvider = IntegrationProvider.BILLETWEB

    override suspend fun createTickets(
        integrationId: UUID,
        eventId: UUID,
        partnershipId: UUID,
        tickets: List<TicketData>,
    ): TicketOrder {
        val config = transaction { BilletWebIntegrationsTable[integrationId] }
        val billing = transaction { BillingEntity.singleByEventAndPartnership(eventId, partnershipId) }
            ?: throw NotFoundException("Billing entity not found for event $eventId and partnership $partnershipId")
        val order = billetWebProvider.createOrder(billing.toOrderRequest(tickets, config), config)
        return order.toDomain(data = tickets)
    }

    override suspend fun updateTicket(integrationId: UUID, ticketId: String, data: TicketData): Ticket {
        val config = transaction { BilletWebIntegrationsTable[integrationId] }
        val ticket = transaction { PartnershipTicketEntity.findById(ticketId) }
            ?: throw NotFoundException("Ticket with id $ticketId not found")
        billetWebProvider.updateProduct(ticket.toCreateOrderProduct(config), config)
        return ticket.toDomain()
    }
}
