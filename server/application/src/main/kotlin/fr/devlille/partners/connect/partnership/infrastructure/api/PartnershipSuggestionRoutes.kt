package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.NotificationPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.WebhookPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.ktor.variables
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
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

    route("/events/{eventSlug}/partnerships/{partnershipId}/suggestion-approve") {
        install(NotificationPartnershipPlugin)
        install(WebhookPartnershipPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val id = suggestionRepository.approve(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val variables = NotificationVariables.SuggestionApproved(
                language = partnership.language,
                event = eventRepository.getBySlug(eventSlug),
                company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId),
                partnership = partnership,
            )
            call.attributes.variables = variables
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
    route("/events/{eventSlug}/partnerships/{partnershipId}/suggestion-decline") {
        install(NotificationPartnershipPlugin)
        install(WebhookPartnershipPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val id = suggestionRepository.decline(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val variables = NotificationVariables.SuggestionDeclined(
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

fun Route.orgsPartnershipSuggestionRoutes() {
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val suggestionRepository by inject<PartnershipSuggestionRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/suggestion") {
        install(AuthorizedOrganisationPlugin)
        install(NotificationPartnershipPlugin)
        install(WebhookPartnershipPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val input = call.receive<SuggestPartnership>(schema = "suggest_partnership.schema.json")
            val id = suggestionRepository.suggest(eventSlug, partnershipId, input)
            val partnership = partnershipRepository.getById(eventSlug, id)
            val variables = NotificationVariables.NewSuggestion(
                language = input.language,
                event = eventRepository.getBySlug(eventSlug),
                company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId),
                partnership = partnership,
                pack = partnership.suggestionPack
                    ?: throw NotFoundException("Partnership does not have a suggestion pack"),
            )
            call.attributes.variables = variables
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
}
