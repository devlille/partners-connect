package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.ktor.NotificationPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.WebhookPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.variables
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipDecisionRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.orgsPartnershipDecisionRoutes() {
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val partnershipDecisionRepository by inject<PartnershipDecisionRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/validate") {
        install(AuthorizedOrganisationPlugin)
        install(NotificationPartnershipPlugin)
        install(WebhookPartnershipPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val id = partnershipDecisionRepository.validate(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val variables = NotificationVariables.PartnershipValidated(
                language = partnership.language,
                event = eventRepository.getBySlug(eventSlug),
                company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId),
                partnership = partnership,
                pack = partnership.selectedPack
                    ?: throw ForbiddenException("Partnership does not have a selected pack"),
            )
            call.attributes.variables = variables
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/decline") {
        install(AuthorizedOrganisationPlugin)
        install(NotificationPartnershipPlugin)
        install(WebhookPartnershipPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val id = partnershipDecisionRepository.decline(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val variables = NotificationVariables.PartnershipDeclined(
                language = partnership.language,
                event = eventRepository.getBySlug(eventSlug),
                company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId),
                partnership = partnership,
            )
            call.attributes.variables = variables
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
}
