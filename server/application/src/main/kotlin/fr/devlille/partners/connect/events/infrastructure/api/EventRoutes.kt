package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.organisations.infrastructure.api.orgSlug
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.eventRoutes() {
    val repository by inject<EventRepository>()

    route("events") {
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            val paginated = repository.getAllEvents(page, pageSize)
            call.respond(HttpStatusCode.OK, paginated)
        }

        get("/{eventSlug}") {
            val eventSlug = call.parameters.eventSlug
            val eventWithOrg = repository.getBySlug(eventSlug)
            call.respond(HttpStatusCode.OK, eventWithOrg)
        }
    }

    route("orgs/{orgSlug}/events") {
        install(AuthorizedOrganisationPlugin)

        get {
            val orgSlug = call.parameters.orgSlug
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            val paginated = repository.findByOrgSlugPaginated(orgSlug, page, pageSize)
            call.respond(HttpStatusCode.OK, paginated)
        }

        post {
            val orgSlug = call.parameters.orgSlug
            val request = call.receive<Event>(schema = "create_event.schema.json")
            val slug = repository.createEvent(orgSlug, request)
            call.respond(
                status = HttpStatusCode.Created,
                message = mapOf("slug" to slug),
            )
        }

        put("/{eventSlug}") {
            val orgSlug = call.parameters.orgSlug
            val eventSlug = call.parameters.eventSlug
            val updatedSlug = repository
                .updateEvent(eventSlug, orgSlug, call.receive<Event>(schema = "create_event.schema.json"))
            call.respond(
                status = HttpStatusCode.OK,
                message = mapOf("slug" to updatedSlug),
            )
        }
    }

    publicEventAgendaRoutes()
    orgsEventAgendaRoutes()
}
