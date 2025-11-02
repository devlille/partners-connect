package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipDecisionRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
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
    val notificationRepository by inject<NotificationRepository>()
    val webhookRepository by inject<WebhookRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}") {
        route("/validate") {
            install(AuthorizedOrganisationPlugin)

            post {
                val eventSlug = call.parameters.eventSlug
                val partnershipId = call.parameters.partnershipId
                val id = partnershipDecisionRepository.validate(eventSlug, partnershipId)
                val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
                val partnership = partnershipRepository.getById(eventSlug, partnershipId)
                val pack = partnership.selectedPack
                    ?: throw ForbiddenException("Partnership does not have a selected pack")
                val event = eventRepository.getBySlug(eventSlug)
                val variables = NotificationVariables.PartnershipValidated(
                    partnership.language,
                    event,
                    company,
                    partnership,
                    pack,
                )
                notificationRepository.sendMessage(eventSlug, variables)

                // Send webhook notification for partnership validation
                webhookRepository.sendWebhooks(eventSlug, partnershipId, WebhookEventType.UPDATED)

                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }

        route("/decline") {
            install(AuthorizedOrganisationPlugin)

            post {
                val eventSlug = call.parameters.eventSlug
                val partnershipId = call.parameters.partnershipId
                val id = partnershipDecisionRepository.decline(eventSlug, partnershipId)
                val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
                val partnership = partnershipRepository.getById(eventSlug, partnershipId)
                val event = eventRepository.getBySlug(eventSlug)
                val variables = NotificationVariables.PartnershipDeclined(
                    partnership.language,
                    event,
                    company,
                    partnership,
                )
                notificationRepository.sendMessage(eventSlug, variables)

                // Send webhook notification for partnership decline
                webhookRepository.sendWebhooks(eventSlug, partnershipId, WebhookEventType.UPDATED)

                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }
    }
}
