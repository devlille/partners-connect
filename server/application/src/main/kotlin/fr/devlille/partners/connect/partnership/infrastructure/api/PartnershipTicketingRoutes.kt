package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.partnershipTicketingRoutes() {
    val ticketingRepository by inject<TicketRepository>()

    route("/events/{eventSlug}/partnerships/{partnershipId}/tickets") {
        get {
            val partnershipId = call.parameters.partnershipId
            val tickets = ticketingRepository.listTickets(partnershipId)
            if (tickets.isEmpty()) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.OK, tickets)
            }
        }

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val tickets = call.receive<List<TicketData>>(schema = "create_ticket_data.schema.json")
            val result = ticketingRepository.createTickets(eventSlug, partnershipId, tickets)
            call.respond(HttpStatusCode.Created, result)
        }

        put("/{ticketId}") {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val ticketId = call.parameters.ticketId
            val body = call.receive<TicketData>(schema = "ticket_data.schema.json")
            val ticket = ticketingRepository.updateTicket(eventSlug, partnershipId, ticketId, body)
            call.respond(HttpStatusCode.OK, ticket)
        }
    }
}
