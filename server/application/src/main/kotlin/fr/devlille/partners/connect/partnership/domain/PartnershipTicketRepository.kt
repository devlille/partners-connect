package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import java.util.UUID

interface PartnershipTicketRepository {
    fun create(eventSlug: String, partnershipId: UUID, order: TicketOrder): List<String>

    fun update(ticket: Ticket, input: TicketData): String
}
