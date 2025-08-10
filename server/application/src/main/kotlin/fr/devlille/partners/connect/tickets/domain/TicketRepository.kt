package fr.devlille.partners.connect.tickets.domain

import java.util.UUID

interface TicketRepository {
    fun listTickets(partnershipId: UUID): List<Ticket>

    suspend fun createTickets(eventId: UUID, partnershipId: UUID, tickets: List<TicketData>): TicketOrder

    suspend fun updateTicket(eventId: UUID, partnershipId: UUID, ticketId: String, data: TicketData): Ticket
}
