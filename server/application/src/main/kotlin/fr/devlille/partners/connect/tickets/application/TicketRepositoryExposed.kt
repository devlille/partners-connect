package fr.devlille.partners.connect.tickets.application

import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.tickets.domain.TicketGateway
import fr.devlille.partners.connect.tickets.domain.TicketRepository
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class TicketRepositoryExposed(
    private val gateways: List<TicketGateway>,
) : TicketRepository {
    override suspend fun createTickets(eventId: UUID, partnershipId: UUID): String = newSuspendedTransaction {
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
        val gateway = gateways.find { it.provider == provider }
            ?: throw NotFoundException("No gateway for provider $provider")
        gateway.createTickets(integrationId, eventId, partnershipId)
    }
}
