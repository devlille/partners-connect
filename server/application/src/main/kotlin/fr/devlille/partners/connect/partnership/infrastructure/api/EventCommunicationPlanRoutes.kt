package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Route.eventCommunicationPlanRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/communication") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")

            val communicationPlan = partnershipRepository.listCommunicationPlan(eventSlug)
            call.respond(HttpStatusCode.OK, communicationPlan)
        }
    }
}
