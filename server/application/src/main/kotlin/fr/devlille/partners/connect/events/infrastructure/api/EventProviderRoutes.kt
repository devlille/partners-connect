package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.provider.domain.ProviderRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.eventProviderRoutes() {
    val providerRepository by inject<ProviderRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/providers") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val providerIdStrings = call.receive<List<String>>()

            // Validate and convert provider IDs
            val providerIds = providerIdStrings.map { it.toUUID() }

            if (providerIds.isEmpty()) {
                throw BadRequestException("Provider IDs list cannot be empty")
            }

            val attachedIds = providerRepository.attachToEvent(eventSlug, providerIds)
            call.respond(HttpStatusCode.OK, attachedIds.map { it.toString() })
        }
    }
}
