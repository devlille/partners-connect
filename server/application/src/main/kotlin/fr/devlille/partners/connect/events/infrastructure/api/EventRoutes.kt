package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
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

private const val DEFAULT_PAGE_SIZE = 20

@Suppress("ThrowsCount")
fun Route.eventRoutes() {
    val repository by inject<EventRepository>()

    route("events") {
        get {
            call.respond(repository.getAllEvents())
        }

        get("/{event_slug}") {
            val eventSlug = call.parameters["event_slug"] ?: throw BadRequestException("Missing event slug")
            val eventWithOrg = repository.getBySlug(eventSlug)
            call.respond(HttpStatusCode.OK, eventWithOrg)
        }
    }

    route("orgs/{orgSlug}/events") {
        install(AuthorizedOrganisationPlugin)

        get {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Missing organisation slug")
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            val paginated = repository.findByOrgSlugPaginated(orgSlug, page, pageSize)
            call.respond(HttpStatusCode.OK, paginated)
        }

        post {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Missing organisation slug")
            val request = call.receive<Event>()
            val slug = repository.createEvent(orgSlug, request)
            call.respond(
                status = HttpStatusCode.Created,
                message = mapOf("slug" to slug),
            )
        }

        put("/{eventSlug}") {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Missing organisation slug")
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val updatedSlug = repository.updateEvent(eventSlug, orgSlug, call.receive<Event>())
            call.respond(
                status = HttpStatusCode.OK,
                message = mapOf("slug" to updatedSlug),
            )
        }
    }
}
