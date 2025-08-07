package fr.devlille.partners.connect.tickets.domain

import java.util.UUID

interface TicketRepository {
    suspend fun createTickets(eventId: UUID, partnershipId: UUID): String
}
