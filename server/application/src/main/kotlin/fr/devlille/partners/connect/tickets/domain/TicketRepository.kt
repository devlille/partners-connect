package fr.devlille.partners.connect.tickets.domain

import java.util.UUID

interface TicketRepository {
    fun listTickets(partnershipId: UUID): List<Ticket>

    suspend fun createTickets(eventSlug: String, partnershipId: UUID, tickets: List<TicketData>): TicketOrder

    suspend fun updateTicket(eventSlug: String, partnershipId: UUID, ticketId: String, input: TicketData): Ticket
}
