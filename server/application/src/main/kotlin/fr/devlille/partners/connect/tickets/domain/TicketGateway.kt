package fr.devlille.partners.connect.tickets.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

interface TicketGateway {
    val provider: IntegrationProvider

    suspend fun createTickets(
        integrationId: UUID,
        eventId: UUID,
        partnershipId: UUID,
        tickets: List<TicketData>,
    ): TicketOrder

    suspend fun updateTicket(integrationId: UUID, ticketId: String, data: TicketData): Ticket
}
