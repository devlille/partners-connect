package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.eventRoutes() {
    val repository by inject<EventRepository>()

    route("events") {
        get {
            call.respond(repository.getAllEvents())
        }
    }

    route("orgs/{orgSlug}/events") {
        install(AuthorizedOrganisationPlugin)

        post {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Missing organisation slug")
            val request = call.receive<Event>()
            val id = repository.createEvent(orgSlug, request)
            call.respond(
                status = HttpStatusCode.Created,
                message = mapOf("id" to id.toString()),
            )
        }

        put("/{eventId}") {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Missing organisation slug")
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing id")
            repository.updateEvent(eventId, orgSlug, call.receive<Event>())
            call.respond(
                status = HttpStatusCode.OK,
                message = mapOf("id" to eventId.toString()),
            )
        }
    }
}
