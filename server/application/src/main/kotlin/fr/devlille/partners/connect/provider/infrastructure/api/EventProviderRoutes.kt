package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.organisations.infrastructure.api.orgSlug
import fr.devlille.partners.connect.provider.domain.ProviderRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.orgsEventProviderRoutes() {
    val providerRepository by inject<ProviderRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/providers") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters.eventSlug
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE

            val eventProviders = providerRepository.findByEvent(eventSlug, page, pageSize)
            call.respond(HttpStatusCode.OK, eventProviders)
        }

        post {
            val eventSlug = call.parameters.eventSlug
            val orgSlug = call.parameters.orgSlug
            val providerIds = call.receive<List<String>>(schema = "create_by_identifiers.schema.json")
                .map { it.toUUID() }
            val attachedIds = providerRepository.attachToEvent(orgSlug, eventSlug, providerIds)

            // Return list of attached provider IDs as strings
            call.respond(HttpStatusCode.OK, attachedIds.map { it.toString() })
        }

        delete {
            val eventSlug = call.parameters.eventSlug
            val orgSlug = call.parameters.orgSlug
            val providerIds = call.receive<List<String>>(schema = "create_by_identifiers.schema.json")
                .map { it.toUUID() }
            val detachedIds = providerRepository.detachFromEvent(orgSlug, eventSlug, providerIds)
            call.respond(HttpStatusCode.OK, detachedIds.map { it.toString() })
        }
    }
}
