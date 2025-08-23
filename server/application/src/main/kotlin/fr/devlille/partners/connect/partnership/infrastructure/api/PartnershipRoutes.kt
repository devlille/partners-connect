package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipFilters
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount", "LongMethod")
fun Route.partnershipRoutes() {
    val eventRepository by inject<EventRepository>()
    val companyRepository by inject<CompanyRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventSlug}/partnership") {
        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                code = ErrorCode.BAD_REQUEST,
                message = "Missing event slug",
            )
            val register = call.receive<RegisterPartnership>()
            val id = partnershipRepository.register(eventSlug, register)
            val company = companyRepository.getById(register.companyId.toUUID())
            val partnership = partnershipRepository.getById(eventSlug, id)
            val pack = partnership.selectedPack
                ?: throw NotFoundException(
                    code = ErrorCode.ENTITY_NOT_FOUND,
                    message = "Partnership does not have a selected pack",
                )
            val event = eventRepository.getBySlug(eventSlug)
            val variables = NotificationVariables.NewPartnership(
                register.language,
                event,
                company,
                partnership,
                pack,
            )
            notificationRepository.sendMessage(eventSlug, variables)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/partnership") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                code = ErrorCode.BAD_REQUEST,
                message = "Missing event slug",
            )

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
                val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                    code = ErrorCode.BAD_REQUEST,
                    message = "Missing event slug",
                )
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException(
                        code = ErrorCode.BAD_REQUEST,
                        message = "Missing partnership id",
                    )
                val id = partnershipRepository.validate(eventSlug, partnershipId)
                val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
                val partnership = partnershipRepository.getById(eventSlug, partnershipId)
                val pack = partnership.selectedPack
                    ?: throw BadRequestException(
                        code = ErrorCode.BAD_REQUEST,
                        message = "Partnership does not have a selected pack",
                    )
                val event = eventRepository.getBySlug(eventSlug)
                val variables = NotificationVariables.PartnershipValidated(
                    partnership.language,
                    event,
                    company,
                    partnership,
                    pack,
                )
                notificationRepository.sendMessage(eventSlug, variables)
                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }

        route("/decline") {
            install(AuthorizedOrganisationPlugin)

            post {
                val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                    code = ErrorCode.BAD_REQUEST,
                    message = "Missing event slug",
                )
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException(
                        code = ErrorCode.BAD_REQUEST,
                        message = "Missing partnership id",
                    )
                val id = partnershipRepository.decline(eventSlug, partnershipId)
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
                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }
    }
}
