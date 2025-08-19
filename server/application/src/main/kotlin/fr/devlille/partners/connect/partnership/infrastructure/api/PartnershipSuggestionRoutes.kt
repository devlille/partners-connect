package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.partnershipSuggestionRoutes() {
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val suggestionRepository by inject<PartnershipSuggestionRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventSlug}/partnership/{partnershipId}") {
        post("/suggestion-approve") {
            handleSuggestionApproval(
                eventRepository,
                partnershipRepository,
                suggestionRepository,
                notificationRepository,
                call,
            )
        }

        post("/suggestion-decline") {
            handleSuggestionDecline(
                eventRepository,
                partnershipRepository,
                suggestionRepository,
                notificationRepository,
                call,
            )
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}") {
        install(AuthorizedOrganisationPlugin)

        post("/suggestion") {
            handleNewSuggestion(
                eventRepository,
                partnershipRepository,
                suggestionRepository,
                notificationRepository,
                call,
            )
        }
    }
}

private suspend fun handleSuggestionApproval(
    eventRepository: EventRepository,
    partnershipRepository: PartnershipRepository,
    suggestionRepository: PartnershipSuggestionRepository,
    notificationRepository: NotificationRepository,
    call: ApplicationCall,
) {
    val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
    val partnershipId = call.parameters["partnershipId"]?.toUUID()
        ?: throw BadRequestException("Missing partnership id")
    val id = suggestionRepository.approve(eventSlug, partnershipId)
    val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
    val partnership = partnershipRepository.getById(eventSlug, partnershipId)
    val eventWithOrganisation = eventRepository.getBySlug(eventSlug)
    val partnershipContext = NotificationVariables.PartnershipContext(
        eventWithOrganisation,
        eventSlug,
        partnershipId,
    )
    val variables = NotificationVariables.SuggestionApproved(
        partnership.language,
        eventWithOrganisation.event,
        company,
        partnershipContext,
    )
    notificationRepository.sendMessage(eventSlug, variables)
    call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
}

private suspend fun handleSuggestionDecline(
    eventRepository: EventRepository,
    partnershipRepository: PartnershipRepository,
    suggestionRepository: PartnershipSuggestionRepository,
    notificationRepository: NotificationRepository,
    call: ApplicationCall,
) {
    val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
    val partnershipId = call.parameters["partnershipId"]?.toUUID()
        ?: throw BadRequestException("Missing partnership id")
    val id = suggestionRepository.decline(eventSlug, partnershipId)
    val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
    val partnership = partnershipRepository.getById(eventSlug, partnershipId)
    val eventWithOrganisation = eventRepository.getBySlug(eventSlug)
    val partnershipContext = NotificationVariables.PartnershipContext(
        eventWithOrganisation,
        eventSlug,
        partnershipId,
    )
    val variables = NotificationVariables.SuggestionDeclined(
        partnership.language,
        eventWithOrganisation.event,
        company,
        partnershipContext,
    )
    notificationRepository.sendMessage(eventSlug, variables)
    call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
}

@Suppress("ThrowsCount")
private suspend fun handleNewSuggestion(
    eventRepository: EventRepository,
    partnershipRepository: PartnershipRepository,
    suggestionRepository: PartnershipSuggestionRepository,
    notificationRepository: NotificationRepository,
    call: ApplicationCall,
) {
    val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
    val partnershipId = call.parameters["partnershipId"]?.toUUID()
        ?: throw BadRequestException("Missing partnership id")
    val input = call.receive<SuggestPartnership>()
    val id = suggestionRepository.suggest(eventSlug, partnershipId, input)
    val partnership = partnershipRepository.getById(eventSlug, id)
    val pack = partnership.suggestionPack
        ?: throw NotFoundException("Partnership does not have a suggestion pack")
    val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
    val eventWithOrganisation = eventRepository.getBySlug(eventSlug)
    val partnershipContext = NotificationVariables.PartnershipContext(
        eventWithOrganisation,
        eventSlug,
        id,
    )
    val variables = NotificationVariables.NewSuggestion(
        input.language,
        eventWithOrganisation.event,
        company,
        pack,
        partnershipContext,
    )
    notificationRepository.sendMessage(eventSlug, variables)
    call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
}
