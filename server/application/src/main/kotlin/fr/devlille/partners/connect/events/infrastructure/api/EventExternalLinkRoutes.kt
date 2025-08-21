package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.CreateEventExternalLinkRequest
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
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
            val request = call.receive<CreateEventExternalLinkRequest>()

            // Basic validation
            if (request.name.isBlank()) {
                throw BadRequestException("External link name cannot be empty")
            }
            if (request.url.isBlank()) {
                throw BadRequestException("External link URL cannot be empty")
            }

            // Basic URL validation
            val urlPattern = Regex("^https?://.*")
            if (!urlPattern.matches(request.url)) {
                throw BadRequestException("Invalid URL format - must start with http:// or https://")
            }

            val externalLink = repository.createExternalLink(eventSlug, request)
            call.respond(HttpStatusCode.Created, externalLink)
        }
    }
}
