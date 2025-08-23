package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.CreateEventExternalLinkRequest
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
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
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val request = call.receive<CreateEventExternalLinkRequest>()

            val externalLinkId = repository.createExternalLink(eventSlug, request)
            call.respond(HttpStatusCode.Created, mapOf("id" to externalLinkId.toString()))
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/external-link/{linkId}") {
        install(AuthorizedOrganisationPlugin)

        delete {
            val linkId = call.parameters["linkId"] ?: throw BadRequestException(
                message = "Missing link ID",
            )

            repository.deleteExternalLink(linkId.toUUID())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
