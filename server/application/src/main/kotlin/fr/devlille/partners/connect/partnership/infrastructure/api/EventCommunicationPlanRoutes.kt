package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.eventCommunicationPlanRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/communication") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters.eventSlug
            val communicationPlan = partnershipRepository.listCommunicationPlan(eventSlug)
            call.respond(HttpStatusCode.OK, communicationPlan)
        }
    }
}
