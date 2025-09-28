package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.CreateEventExternalLinkRequest
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.eventExternalLinkRoutes() {
    val repository by inject<EventRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/external-link") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val request = call
                .receive<CreateEventExternalLinkRequest>(schema = "create_event_external_link.schema.json")
            val externalLinkId = repository.createExternalLink(eventSlug, request)
            call.respond(HttpStatusCode.Created, mapOf("id" to externalLinkId.toString()))
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/external-link/{linkId}") {
        install(AuthorizedOrganisationPlugin)

        delete {
            val linkId = call.parameters["linkId"] ?: throw BadRequestException("Missing link ID")

            repository.deleteExternalLink(linkId.toUUID())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
