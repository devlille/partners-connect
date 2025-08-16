package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipFilters
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
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
    }

    route("/orgs/{orgSlug}/events/{eventId}/partnership") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")

            // Verify event exists
            eventRepository.getById(eventId)

            // Parse query parameters for filters
            val filters = PartnershipFilters(
                packId = call.request.queryParameters["filter[pack_id]"],
                validated = call.request.queryParameters["filter[validated]"]?.toBoolean(),
                suggestion = call.request.queryParameters["filter[suggestion]"]?.toBoolean(),
                paid = call.request.queryParameters["filter[paid]"]?.toBoolean(),
                agreementGenerated = call.request.queryParameters["filter[agreement-generated]"]?.toBoolean(),
                agreementSigned = call.request.queryParameters["filter[agreement-signed]"]?.toBoolean(),
            )

            val sort = call.request.queryParameters["sort"] ?: "created"
            val direction = call.request.queryParameters["direction"] ?: "asc"

            val partnerships = partnershipRepository.listByEvent(eventId, filters, sort, direction)
            call.respond(HttpStatusCode.OK, partnerships)
        }
    }

    route("/orgs/{orgSlug}/events/{eventId}/partnership/{partnershipId}") {
        route("/validate") {
            install(AuthorizedOrganisationPlugin)

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

        route("/decline") {
            install(AuthorizedOrganisationPlugin)

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
