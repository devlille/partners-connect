package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.NotificationPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.variables
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.FlyerGenerationRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.orgsPartnershipFlyerRoutes() {
    val repository by inject<FlyerGenerationRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val eventRepository by inject<EventRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/flyer") {
        install(AuthorizedOrganisationPlugin)
        install(NotificationPartnershipPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val flyer = repository.generate(eventSlug, partnershipId)

            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            call.attributes.variables = NotificationVariables.FlyerGenerated(
                language = partnership.language,
                event = eventRepository.getBySlug(eventSlug),
                company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId),
                partnership = partnership,
                flyerUrl = flyer.url,
            )
            call.respond(HttpStatusCode.OK, mapOf("url" to flyer.url))
        }
    }
}
