package fr.devlille.partners.connect.tickets.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.getTotalTicketsFromOptions
import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketGateway
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import fr.devlille.partners.connect.tickets.domain.TicketRepository
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class TicketRepositoryExposed(
    private val gateways: List<TicketGateway>,
) : TicketRepository {
    override fun listTickets(partnershipId: UUID): List<Ticket> = transaction {
        val tickets = PartnershipTicketEntity.listByPartnership(partnershipId)
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

    override suspend fun createTickets(
        eventSlug: String,
        partnershipId: UUID,
        tickets: List<TicketData>,
    ): TicketOrder {
        val event = transaction { EventEntity.findBySlug(eventSlug) }
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val integration = transaction {
            val billing = BillingEntity.singleByEventAndPartnership(eventId, partnershipId)
                ?: throw NotFoundException("Billing entity not found for event $eventId and partnership $partnershipId")
            if (billing.status != InvoiceStatus.PAID) {
                throw ForbiddenException("Invoice status ${billing.status} is not PAID")
            }
            val partnership = billing.partnership
            val validatedPack = partnership.validatedPack()
            if (validatedPack == null) {
                throw NotFoundException("No validated pack found for partnership ${partnership.id}")
            } else {
                val nbTickets = validatedPack.getTotalTicketsFromOptions()
                if (tickets.size > nbTickets) {
                    val message = """
    Not enough tickets in the validated pack: $nbTickets available, ${tickets.size} requested
                    """.trimIndent()
                    throw ForbiddenException(message)
                }
            }
            IntegrationEntity.singleIntegration(event.id.value, IntegrationUsage.TICKETING)
        }
        val gateway = gateways.find { it.provider == integration.provider }
            ?: throw NotFoundException("No gateway for provider ${integration.provider}")
        return gateway.createTickets(integration.id.value, eventId, partnershipId, tickets)
    }

    override suspend fun updateTicket(
        eventSlug: String,
        partnershipId: UUID,
        ticketId: String,
        input: TicketData,
    ): Ticket {
        val integration = transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            IntegrationEntity.singleIntegration(event.id.value, IntegrationUsage.TICKETING)
        }
        val gateway = gateways.find { it.provider == integration.provider }
            ?: throw NotFoundException("No gateway for provider ${integration.provider}")
        return gateway.updateTicket(integration.id.value, ticketId, input)
    }
}
