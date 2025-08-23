package fr.devlille.partners.connect.tickets.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
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
            throw NotFoundException(
                code = ErrorCode.TICKETS_NOT_FOUND,
                message = "No tickets found for partnership $partnershipId",
                meta = mapOf(MetaKeys.PARTNERSHIP_ID to partnershipId.toString()),
            )
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

    @Suppress("LongMethod")
    override suspend fun createTickets(eventSlug: String, partnershipId: UUID, tickets: List<TicketData>): TicketOrder {
        val (eventId, provider, integrationId) = getEventAndIntegration(eventSlug)
        val gateway = gateways.find { it.provider == provider }
            ?: throw NotFoundException(
                code = ErrorCode.PROVIDER_NOT_FOUND,
                message = "No gateway for provider $provider",
                meta = mapOf(MetaKeys.PROVIDER to provider.name),
            )

        // Validate billing entity exists and is paid
        val billing = transaction { BillingEntity.singleByEventAndPartnership(eventId, partnershipId) }
            ?: throw NotFoundException(
                code = ErrorCode.BILLING_NOT_FOUND,
                message = "Billing entity not found for event $eventId and partnership $partnershipId",
                meta = mapOf(
                    MetaKeys.EVENT to eventSlug,
                    MetaKeys.PARTNERSHIP_ID to partnershipId.toString(),
                ),
            )
        if (billing.status != InvoiceStatus.PAID) {
            throw ForbiddenException(
                code = ErrorCode.BILLING_PROCESSING_ERROR,
                message = "Invoice status ${billing.status} is not PAID",
                meta = mapOf(
                    MetaKeys.INVOICE_STATUS to billing.status.name,
                    MetaKeys.REQUIRED_STATUS to "PAID",
                    MetaKeys.EVENT to eventSlug,
                    MetaKeys.PARTNERSHIP_ID to partnershipId.toString(),
                ),
            )
        }
        val validatedPack = transaction { billing.partnership.validatedPack() }
        if (validatedPack == null) {
            throw NotFoundException(
                code = ErrorCode.VALIDATED_PACK_NOT_FOUND,
                message = "No validated pack found for partnership $partnershipId",
                meta = mapOf(MetaKeys.PARTNERSHIP_ID to partnershipId.toString()),
            )
        }
        if (validatedPack.nbTickets < tickets.size) {
            val message = "Not enough tickets in the validated pack: " +
                "${validatedPack.nbTickets} available, ${tickets.size} requested"
            throw ForbiddenException(
                code = ErrorCode.TICKET_GENERATION_ERROR,
                message = message,
                meta = mapOf(
                    MetaKeys.AVAILABLE_TICKETS to validatedPack.nbTickets.toString(),
                    MetaKeys.REQUESTED_TICKETS to tickets.size.toString(),
                    MetaKeys.PARTNERSHIP_ID to partnershipId.toString(),
                ),
            )
        }
        val partnership = transaction { billing.partnership }

        val order = gateway.createTickets(integrationId, eventId, partnershipId, tickets)
        persistTicketsToDatabase(order, partnership, billing.contactEmail)
        return order
    }

    private fun getEventAndIntegration(eventSlug: String): Triple<UUID, IntegrationProvider, UUID> {
        return transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException(
                    code = ErrorCode.EVENT_NOT_FOUND,
                    message = "Event with slug $eventSlug not found",
                    meta = mapOf(MetaKeys.EVENT to eventSlug),
                )
            val eventId = event.id.value
            val integration = singleIntegrationWithinTransaction(eventId)
            val provider = integration[IntegrationsTable.provider]
            val integrationId = integration[IntegrationsTable.id].value
            Triple(eventId, provider, integrationId)
        }
    }

    private fun persistTicketsToDatabase(
        order: TicketOrder,
        partnership: fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity,
        contactEmail: String,
    ) {
        transaction {
            order.tickets.forEach { ticket ->
                PartnershipTicketEntity.new(ticket.id) {
                    this.partnership = partnership
                    this.orderId = order.id
                    this.externalId = ticket.extId
                    this.url = ticket.url
                    this.firstname = ticket.data.firstName
                    this.lastname = ticket.data.lastName
                    this.email = contactEmail
                }
            }
        }
    }

    override suspend fun updateTicket(
        eventSlug: String,
        partnershipId: UUID,
        ticketId: String,
        data: TicketData,
    ): Ticket {
        val (eventId, provider, integrationId) = transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException(
                    code = ErrorCode.EVENT_NOT_FOUND,
                    message = "Event with slug $eventSlug not found",
                    meta = mapOf(MetaKeys.EVENT to eventSlug),
                )
            val eventId = event.id.value
            val integration = singleIntegrationWithinTransaction(eventId)
            val provider = integration[IntegrationsTable.provider]
            val integrationId = integration[IntegrationsTable.id].value
            Triple(eventId, provider, integrationId)
        }

        val gateway = gateways.find { it.provider == provider }
            ?: throw NotFoundException(
                code = ErrorCode.PROVIDER_NOT_FOUND,
                message = "No gateway for provider $provider",
                meta = mapOf(MetaKeys.PROVIDER to provider.name),
            )
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

    private fun singleIntegrationWithinTransaction(eventId: UUID): ResultRow {
        val integrations = IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.TICKETING)
            .toList()
        if (integrations.isEmpty()) {
            throw NotFoundException(
                code = ErrorCode.TICKETS_NOT_FOUND,
                message = "No ticketing integration found for event $eventId",
                meta = mapOf(MetaKeys.EVENT_ID to eventId.toString()),
            )
        }
        if (integrations.size > 1) {
            throw ConflictException(
                code = ErrorCode.MULTIPLE_INTEGRATIONS_FOUND,
                message = "Multiple ticketing integrations found for event $eventId",
                meta = mapOf(MetaKeys.EVENT_ID to eventId.toString()),
            )
        }
        return integrations.single()
    }
}
