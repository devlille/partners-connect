package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedEventPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

@Suppress("ThrowsCount")
fun Route.partnershipSuggestionRoutes() {
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val suggestionRepository by inject<PartnershipSuggestionRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventId}/partnership/{partnershipId}") {
        route("/suggestion") {
            install(AuthorizedEventPlugin)
            post {
                val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException("Missing partnership id")
                val input = call.receive<SuggestPartnership>()
                val id = suggestionRepository.suggest(eventId, partnershipId, input)
                val partnership = partnershipRepository.getById(eventId, id)
                val pack = partnership.suggestionPack
                    ?: throw NotFoundException("Partnership does not have a suggestion pack")
                val company = partnershipRepository.getCompanyByPartnershipId(eventId, partnershipId)
                val event = eventRepository.getById(eventId)
                val variables = NotificationVariables.NewSuggestion(input.language, event, company, pack)
                notificationRepository.sendMessage(eventId, variables)
                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }
        post("/suggestion-approve") {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val id = suggestionRepository.approve(eventId, partnershipId)
            val company = partnershipRepository.getCompanyByPartnershipId(eventId, partnershipId)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            val event = eventRepository.getById(eventId)
            val variables = NotificationVariables.SuggestionApproved(partnership.language, event, company)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }

        post("/suggestion-decline") {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val id = suggestionRepository.decline(eventId, partnershipId)
            val company = partnershipRepository.getCompanyByPartnershipId(eventId, partnershipId)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            val event = eventRepository.getById(eventId)
            val variables = NotificationVariables.SuggestionDeclined(partnership.language, event, company)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
}
