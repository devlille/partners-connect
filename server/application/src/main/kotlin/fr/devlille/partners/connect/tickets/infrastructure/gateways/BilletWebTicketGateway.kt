package fr.devlille.partners.connect.tickets.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import fr.devlille.partners.connect.tickets.domain.TicketGateway
import io.ktor.client.HttpClient
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BilletWebTicketGateway(
    private val httpClient: HttpClient,
) : TicketGateway {
    override val provider: IntegrationProvider = IntegrationProvider.BILLETWEB

    override suspend fun createTickets(integrationId: UUID, eventId: UUID, partnershipId: UUID): String = transaction {
        val config = BilletWebIntegrationsTable[integrationId]
        val billing = BillingEntity.singleByEventAndPartnership(eventId, partnershipId)
            ?: throw NotFoundException("Billing entity not found for event $eventId and partnership $partnershipId")
        if (billing.status != InvoiceStatus.PAID) {
            throw ForbiddenException("Invoice status ${billing.status} is not PAID")
        }
        TODO("Not yet implemented")
    }
}
