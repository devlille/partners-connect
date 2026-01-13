package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.user
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.notifications.infrastructure.gateways.EmailDeliveryResult
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailHistoryRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.publicPartnershipSuggestionDecisionRoutes() {
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val suggestionRepository by inject<PartnershipSuggestionRepository>()
    val partnershipEmailHistoryRepository by inject<PartnershipEmailHistoryRepository>()
    val notificationRepository by inject<NotificationRepository>()
    val webhookRepository by inject<WebhookRepository>()

    route("/events/{eventSlug}/partnerships/{partnershipId}") {
        post("/suggestion-approve") {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val id = suggestionRepository.approve(eventSlug, partnershipId)
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val event = eventRepository.getBySlug(eventSlug)
            val variables = NotificationVariables.SuggestionApproved(
                partnership.language,
                event,
                company,
                partnership,
            )
            val deliveryResults = notificationRepository.sendMessage(eventSlug, variables)

            // Log email history
            deliveryResults.filterIsInstance<EmailDeliveryResult>().firstOrNull()?.let { deliveryResult ->
                partnershipEmailHistoryRepository.create(
                    partnershipId = partnershipId,
                    senderEmail = deliveryResult.senderEmail,
                    subject = deliveryResult.subject,
                    bodyPlainText = deliveryResult.body,
                    deliveryResult = deliveryResult,
                    triggeredBy = this.call.attributes.user.userId.toUUID(),
                )
            }

            // Send webhook notification for suggestion approval
            webhookRepository.sendWebhooks(eventSlug, partnershipId, WebhookEventType.UPDATED)

            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }

        post("/suggestion-decline") {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val id = suggestionRepository.decline(eventSlug, partnershipId)
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val event = eventRepository.getBySlug(eventSlug)
            val variables = NotificationVariables.SuggestionDeclined(partnership.language, event, company, partnership)
            val deliveryResults = notificationRepository.sendMessage(eventSlug, variables)

            // Log email history
            deliveryResults.filterIsInstance<EmailDeliveryResult>().firstOrNull()?.let { deliveryResult ->
                partnershipEmailHistoryRepository.create(
                    partnershipId = partnershipId,
                    senderEmail = deliveryResult.senderEmail,
                    subject = deliveryResult.subject,
                    bodyPlainText = deliveryResult.body,
                    deliveryResult = deliveryResult,
                    triggeredBy = this.call.attributes.user.userId.toUUID(),
                )
            }

            // Send webhook notification for suggestion decline
            webhookRepository.sendWebhooks(eventSlug, partnershipId, WebhookEventType.UPDATED)

            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
}

fun Route.orgsPartnershipSuggestionRoutes() {
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val suggestionRepository by inject<PartnershipSuggestionRepository>()
    val partnershipEmailHistoryRepository by inject<PartnershipEmailHistoryRepository>()
    val notificationRepository by inject<NotificationRepository>()
    val webhookRepository by inject<WebhookRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/suggestion") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val input = call.receive<SuggestPartnership>(schema = "suggest_partnership.schema.json")
            val id = suggestionRepository.suggest(eventSlug, partnershipId, input)
            val partnership = partnershipRepository.getById(eventSlug, id)
            val pack = partnership.suggestionPack
                ?: throw NotFoundException("Partnership does not have a suggestion pack")
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val event = eventRepository.getBySlug(eventSlug)
            val variables = NotificationVariables.NewSuggestion(
                input.language,
                event,
                company,
                partnership,
                pack,
            )
            val deliveryResults = notificationRepository.sendMessage(eventSlug, variables)

            // Log email history
            deliveryResults.filterIsInstance<EmailDeliveryResult>().firstOrNull()?.let { deliveryResult ->
                partnershipEmailHistoryRepository.create(
                    partnershipId = partnershipId,
                    senderEmail = deliveryResult.senderEmail,
                    subject = deliveryResult.subject,
                    bodyPlainText = deliveryResult.body,
                    deliveryResult = deliveryResult,
                    triggeredBy = this.call.attributes.user.userId.toUUID(),
                )
            }

            // Send webhook notification for new suggestion
            webhookRepository.sendWebhooks(eventSlug, partnershipId, WebhookEventType.UPDATED)

            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
}
