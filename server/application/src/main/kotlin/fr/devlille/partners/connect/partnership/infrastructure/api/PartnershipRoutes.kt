package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedEventPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.partnershipRoutes() {
    val eventRepository by inject<EventRepository>()
    val companyRepository by inject<CompanyRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventId}/partnership") {
        post {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val register = call.receive<RegisterPartnership>()
            val id = partnershipRepository.register(eventId, register)
            val company = companyRepository.getById(register.companyId.toUUID())
            val partnership = partnershipRepository.getById(eventId, id)
            val pack = partnership.selectedPack
                ?: throw NotFoundException("Partnership does not have a selected pack")
            val event = eventRepository.getById(eventId)
            val variables = NotificationVariables.NewPartnership(register.language, event, company, pack)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

        route("/{partnershipId}/validate") {
            install(AuthorizedEventPlugin)

            post {
                val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException("Missing partnership id")
                val id = partnershipRepository.validate(eventId, partnershipId)
                val company = partnershipRepository.getCompanyByPartnershipId(eventId, partnershipId)
                val partnership = partnershipRepository.getById(eventId, partnershipId)
                val pack = partnership.selectedPack
                    ?: throw BadRequestException("Partnership does not have a selected pack")
                val event = eventRepository.getById(eventId)
                val variables = NotificationVariables.PartnershipValidated(partnership.language, event, company, pack)
                notificationRepository.sendMessage(eventId, variables)
                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }

        route("/{partnershipId}/decline") {
            install(AuthorizedEventPlugin)

            post {
                val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException("Missing partnership id")
                val id = partnershipRepository.decline(eventId, partnershipId)
                val company = partnershipRepository.getCompanyByPartnershipId(eventId, partnershipId)
                val partnership = partnershipRepository.getById(eventId, partnershipId)
                val event = eventRepository.getById(eventId)
                val variables = NotificationVariables.PartnershipDeclined(partnership.language, event, company)
                notificationRepository.sendMessage(eventId, variables)
                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }
    }
}
