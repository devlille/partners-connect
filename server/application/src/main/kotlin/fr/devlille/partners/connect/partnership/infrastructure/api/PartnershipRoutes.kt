package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.user
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.notifications.infrastructure.gateways.EmailDeliveryResult
import fr.devlille.partners.connect.partnership.domain.DetailedPartnershipResponse
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailHistoryRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipFilters
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSpeakerRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.domain.UpdatePartnershipContactInfo
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.partnershipRoutes() {
    publicPartnershipRoutes()
    publicPartnershipSuggestionDecisionRoutes()
    publicPartnershipAgreementRoutes()
    publicPartnershipBillingRoutes()
    publicPartnershipTicketingRoutes()
    publicPartnershipJobOfferRoutes()
    publicPartnershipSpeakersRoutes()

    orgsPartnershipRoutes()
    orgsPartnershipDecisionRoutes()
    orgsPartnershipSuggestionRoutes()
    orgsPartnershipAgreementRoutes()
    orgsPartnershipBillingRoutes()
    orgsPartnershipCommunicationRoutes()
    orgsPartnershipBoothLocationRoutes()
    orgsPartnershipJobOfferRoutes()
    orgsPartnershipJobOfferDecisionRoutes()
    orgsPartnershipOrganiserRoutes()
    partnershipEmailRoutes()
    partnershipEmailHistoryRoutes()
    orgsPartnershipWebhookRoutes()
}

@Suppress("LongMethod")
private fun Route.publicPartnershipRoutes() {
    val eventRepository by inject<EventRepository>()
    val companyRepository by inject<CompanyRepository>()
    val speakerRepository by inject<PartnershipSpeakerRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val partnershipEmailHistoryRepository by inject<PartnershipEmailHistoryRepository>()
    val notificationRepository by inject<NotificationRepository>()
    val webhookRepository by inject<WebhookRepository>()

    route("/events/{eventSlug}/partnerships") {
        post {
            val eventSlug = call.parameters.eventSlug
            val register = call.receive<RegisterPartnership>(schema = "register_partnership.schema.json")
            val id = partnershipRepository.register(eventSlug, register)
            val partnership = partnershipRepository.getById(eventSlug, id)
            val variables = NotificationVariables.NewPartnership(
                language = register.language,
                event = eventRepository.getBySlug(eventSlug),
                company = companyRepository.getById(register.companyId.toUUID()),
                partnership = partnership,
                pack = partnership.selectedPack
                    ?: throw NotFoundException("Partnership does not have a selected pack"),
            )
            val deliveryResult = notificationRepository.sendMessage(variables)
                .filterIsInstance<EmailDeliveryResult>()
                .firstOrNull()

            // Log email history
            deliveryResult?.let { deliveryResult ->
                partnershipEmailHistoryRepository.create(
                    partnershipId = id,
                    senderEmail = deliveryResult.senderEmail,
                    subject = deliveryResult.subject,
                    bodyPlainText = deliveryResult.body,
                    deliveryResult = deliveryResult,
                    triggeredBy = this.call.attributes.user.userId.toUUID(),
                )
            }

            // Send webhook notification for partnership creation
            webhookRepository.sendWebhooks(eventSlug, id, WebhookEventType.CREATED)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }
    }
    route("/events/{eventSlug}/partnerships/{partnershipId}") {
        get {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId

            val partnershipDetail = partnershipRepository.getByIdDetailed(eventSlug, partnershipId)
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val speakers = speakerRepository.getSpeakersByPartnership(partnershipId)
            val event = eventRepository.getBySlug(eventSlug)

            val response = DetailedPartnershipResponse(
                partnership = partnershipDetail,
                company = company,
                event = event.event,
                organisation = event.organisation,
                speakers = speakers,
            )

            call.respond(HttpStatusCode.OK, response)
        }

        put {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val update = call.receive<UpdatePartnershipContactInfo>(
                schema = "update_partnership_request.schema.json",
            )

            // Update partnership contact information
            partnershipRepository.updateContactInfo(eventSlug, partnershipId, update)

            // Fetch updated detailed partnership for response
            val partnershipDetail = partnershipRepository.getByIdDetailed(eventSlug, partnershipId)
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val speakers = speakerRepository.getSpeakersByPartnership(partnershipId)
            val event = eventRepository.getBySlug(eventSlug)

            val response = DetailedPartnershipResponse(
                partnership = partnershipDetail,
                company = company,
                event = event.event,
                organisation = event.organisation,
                speakers = speakers,
            )

            call.respond(HttpStatusCode.OK, response)
        }
    }
}

private fun Route.orgsPartnershipRoutes() {
    val repository by inject<PartnershipRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters.eventSlug

            // Parse query parameters for filters
            val filters = PartnershipFilters(
                packId = call.request.queryParameters["filter[pack_id]"],
                validated = call.request.queryParameters["filter[validated]"]?.toBoolean(),
                suggestion = call.request.queryParameters["filter[suggestion]"]?.toBoolean(),
                paid = call.request.queryParameters["filter[paid]"]?.toBoolean(),
                agreementGenerated = call.request.queryParameters["filter[agreement-generated]"]?.toBoolean(),
                agreementSigned = call.request.queryParameters["filter[agreement-signed]"]?.toBoolean(),
                organiser = call.request.queryParameters["filter[organiser]"],
            )

            val direction = call.request.queryParameters["direction"] ?: "asc"

            val partnerships = repository.listByEvent(eventSlug, filters, direction)
            call.respond(HttpStatusCode.OK, partnerships)
        }
    }
    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}") {
        install(AuthorizedOrganisationPlugin)

        delete {
            val partnershipId = call.parameters.partnershipId
            repository.delete(partnershipId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun Route.orgsPartnershipOrganiserRoutes() {
    val repository by inject<PartnershipRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/organiser") {
        install(AuthorizedOrganisationPlugin)

        post {
            val partnershipId = call.parameters.partnershipId
            val request = call.receive<AssignOrganiserRequest>(schema = "assign_organiser_request.schema.json")
            val response = repository.assignOrganiser(partnershipId, request.email)
            call.respond(HttpStatusCode.OK, response)
        }

        delete {
            val partnershipId = call.parameters.partnershipId
            val response = repository.removeOrganiser(partnershipId)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
