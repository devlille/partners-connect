package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.events.application.EventRepositoryExposed
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
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

@Suppress("ThrowsCount", "LongMethod")
fun Route.partnershipRoutes() {
    val eventRepository by inject<EventRepositoryExposed>()
    val companyRepository by inject<CompanyRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventSlug}/partnership") {
        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val register = call.receive<RegisterPartnership>()
            val id = partnershipRepository.register(eventSlug, register)
            val company = companyRepository.getById(register.companyId.toUUID())
            val partnership = partnershipRepository.getById(eventSlug, id)
            val pack = partnership.selectedPack
                ?: throw NotFoundException("Partnership does not have a selected pack")
            val event = eventRepository.getBySlug(eventSlug).event
            val variables = NotificationVariables.NewPartnership(register.language, event, company, pack)
            // Note: we need eventId for notification, get it from event  
            val eventId = EventEntity.findBySlug(eventSlug)?.id?.value
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/partnership") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")

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

            val partnerships = partnershipRepository.listByEvent(eventSlug, filters, sort, direction)
            call.respond(HttpStatusCode.OK, partnerships)
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}") {
        route("/validate") {
            install(AuthorizedOrganisationPlugin)

            post {
                val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException("Missing partnership id")
                val id = partnershipRepository.validate(eventSlug, partnershipId)
                val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
                val partnership = partnershipRepository.getById(eventSlug, partnershipId)
                val pack = partnership.selectedPack
                    ?: throw BadRequestException("Partnership does not have a selected pack")
                val event = eventRepository.getBySlug(eventSlug).event
                val variables = NotificationVariables.PartnershipValidated(partnership.language, event, company, pack)
                // Note: we need eventId for notification, get it from event  
                val eventId = EventEntity.findBySlug(eventSlug)?.id?.value
                    ?: throw NotFoundException("Event with slug $eventSlug not found")
                notificationRepository.sendMessage(eventId, variables)
                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }

        route("/decline") {
            install(AuthorizedOrganisationPlugin)

            post {
                val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException("Missing partnership id")
                val id = partnershipRepository.decline(eventSlug, partnershipId)
                val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
                val partnership = partnershipRepository.getById(eventSlug, partnershipId)
                val event = eventRepository.getBySlug(eventSlug).event
                val variables = NotificationVariables.PartnershipDeclined(partnership.language, event, company)
                // Note: we need eventId for notification, get it from event  
                val eventId = EventEntity.findBySlug(eventSlug)?.id?.value
                    ?: throw NotFoundException("Event with slug $eventSlug not found")
                notificationRepository.sendMessage(eventId, variables)
                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }
    }
}
