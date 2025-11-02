package fr.devlille.partners.connect.tickets.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.getTotalTicketsFromOptions
import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketGateway
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import fr.devlille.partners.connect.tickets.domain.TicketRepository
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
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
    ): TicketOrder = newSuspendedTransaction {
        val event = transaction { EventEntity.findBySlug(eventSlug) }
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
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
        val (provider, integrationId) = singleIntegrationWithinTransaction(event.id.value)
        val gateway = gateways.find { it.provider == provider }
            ?: throw NotFoundException("No gateway for provider $provider")
        gateway.createTickets(integrationId, eventId, partnershipId, tickets)
    }

    override suspend fun updateTicket(
        eventSlug: String,
        partnershipId: UUID,
        ticketId: String,
        input: TicketData,
    ): Ticket {
        val (provider, integrationId) = transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            singleIntegrationWithinTransaction(event.id.value)
        }
        val gateway = gateways.find { it.provider == provider }
            ?: throw NotFoundException("No gateway for provider $provider")
        val ticket = gateway.updateTicket(integrationId, ticketId, input)
        return ticket
    }

    private fun singleIntegrationWithinTransaction(eventId: UUID): Pair<IntegrationProvider, UUID> {
        val integrations = IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.TICKETING)
            .toList()
        if (integrations.isEmpty()) {
            throw NotFoundException("No ticketing integration found for event $eventId")
        }
        if (integrations.size > 1) {
            throw NotFoundException("Multiple ticketing integrations found for event $eventId")
        }
        val integration = integrations.single()
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        return Pair(provider, integrationId)
    }
}
