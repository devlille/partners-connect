package fr.devlille.partners.connect.tickets.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebConfig
import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketGateway
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.CreateOrderProduct
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.CreateOrderRequest
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.CreateOrderResponseItem
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.mappers.toCreateOrderProduct
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.mappers.toDomain
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.mappers.toOrderRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BilletWebTicketGateway(
    private val httpClient: HttpClient,
) : TicketGateway {
    override val provider: IntegrationProvider = IntegrationProvider.BILLETWEB

    override suspend fun createTickets(
        integrationId: UUID,
        eventId: UUID,
        partnershipId: UUID,
        tickets: List<TicketData>,
    ): TicketOrder {
        val config = transaction { BilletWebIntegrationsTable[integrationId] }
        val billing = transaction { BillingEntity.singleByEventAndPartnership(eventId, partnershipId) }
            ?: throw NotFoundException("Billing entity not found for event $eventId and partnership $partnershipId")
        val order = createOrder(billing.toOrderRequest(tickets, config), config)
        return order.toDomain(data = tickets)
    }

    override suspend fun updateTicket(integrationId: UUID, ticketId: String, data: TicketData): Ticket {
        val config = transaction { BilletWebIntegrationsTable[integrationId] }
        val ticket = transaction { PartnershipTicketEntity.findById(ticketId) }
            ?: throw NotFoundException("Ticket with id $ticketId not found")
        updateProduct(ticket.toCreateOrderProduct(config), config)
        return ticket.toDomain()
    }

    suspend fun createOrder(request: CreateOrderRequest, config: BilletWebConfig): CreateOrderResponseItem {
        val route = "https://www.billetweb.fr/api/event/${config.eventId}/add_order"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "Basic ${config.basic}"
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(ListSerializer(CreateOrderRequest.serializer()), listOf(request)))
        }
        return response.body<List<CreateOrderResponseItem>>().first()
    }

    suspend fun updateProduct(request: CreateOrderProduct, config: BilletWebConfig) {
        val route = "https://www.billetweb.fr/api/event/${config.eventId}/update_product"
        httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "Basic ${config.basic}"
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(ListSerializer(CreateOrderProduct.serializer()), listOf(request)))
        }
    }
}
