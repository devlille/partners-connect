package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerDecisionRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.ecosystemPartnerDecisionRoutes() {
    val repository by inject<EcosystemPartnerDecisionRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partners/{ecosystemPartnerId}/validate") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val id = call.parameters.ecosystemPartnerId
            val result = repository.validate(eventSlug, id)
            call.respond(HttpStatusCode.OK, mapOf("id" to result.toString()))
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partners/{ecosystemPartnerId}/decline") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val id = call.parameters.ecosystemPartnerId
            val result = repository.decline(eventSlug, id)
            call.respond(HttpStatusCode.OK, mapOf("id" to result.toString()))
        }
    }
}
