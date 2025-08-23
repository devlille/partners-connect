package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.partnershipTicketingRoutes() {
    val ticketingRepository by inject<TicketRepository>()

    route("/events/{eventSlug}/partnership/{partnershipId}/tickets") {
        get {
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException(
                    message = "Missing partnership id",
                )
            val tickets = ticketingRepository.listTickets(partnershipId)
            call.respond(HttpStatusCode.OK, tickets)
        }

        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException(
                    message = "Missing partnership id",
                )
            val body = call.receive<List<TicketData>>()
            val result = ticketingRepository.createTickets(
                eventSlug = eventSlug,
                partnershipId = partnershipId,
                tickets = body,
            )
            call.respond(HttpStatusCode.Created, result)
        }

        put("/{ticketId}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException(
                    message = "Missing partnership id",
                )
            val ticketId = call.parameters["ticketId"] ?: throw BadRequestException(
                message = "Missing ticket id",
            )
            val body = call.receive<TicketData>()
            val ticket = ticketingRepository.updateTicket(eventSlug, partnershipId, ticketId, body)
            call.respond(HttpStatusCode.OK, ticket)
        }
    }
}
