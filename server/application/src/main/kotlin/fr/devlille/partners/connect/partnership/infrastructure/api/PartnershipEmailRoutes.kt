package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.user
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.infrastructure.gateways.EmailDeliveryResult
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailHistoryRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipFilters
import fr.devlille.partners.connect.partnership.domain.SendPartnershipEmailRequest
import fr.devlille.partners.connect.partnership.domain.SendPartnershipEmailResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Partnership email routes for sending bulk emails to filtered partnerships.
 *
 * Endpoint: POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/email
 *
 * This route orchestrates three repositories:
 * - PartnershipEmailRepository: Fetches partnerships with email contacts
 * - EventRepository: Gets event details for From/CC fallback
 * - NotificationRepository: Sends emails via Mailjet
 *
 * Email grouping by organizer is handled in this route layer to maintain
 * consistent From/CC addresses per batch.
 */
fun Route.partnershipEmailRoutes() {
    val partnershipEmailRepository by inject<PartnershipEmailRepository>()
    val partnershipEmailHistoryRepository by inject<PartnershipEmailHistoryRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/email") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val request = call.receive<SendPartnershipEmailRequest>(
                schema = "send_partnership_email_request.schema.json",
            )

            // Parse query filters
            val filters = PartnershipFilters(
                packId = call.request.queryParameters["filter[pack_id]"],
                validated = call.request.queryParameters["filter[validated]"]?.toBoolean(),
                suggestion = call.request.queryParameters["filter[suggestion]"]?.toBoolean(),
                paid = call.request.queryParameters["filter[paid]"]?.toBoolean(),
                agreementGenerated = call.request.queryParameters["filter[agreement-generated]"]?.toBoolean(),
                agreementSigned = call.request.queryParameters["filter[agreement-signed]"]?.toBoolean(),
                organiser = call.request.queryParameters["filter[organiser]"],
            )

            // Fetch partnerships with emails and organizer contact info
            val destinations = partnershipEmailRepository.getPartnershipDestination(eventSlug, filters)
            if (destinations.isEmpty()) {
                throw NotFoundException("No partnerships match the provided filters")
            }
            destinations.forEach { destination ->
                val deliveryResult = notificationRepository.sendMessage(
                    eventSlug = eventSlug,
                    destination = destination,
                    subject = request.subject,
                    htmlBody = request.body,
                )
                if (deliveryResult is EmailDeliveryResult) {
                    partnershipEmailHistoryRepository.create(
                        partnershipId = destination.partnershipId,
                        senderEmail = deliveryResult.senderEmail,
                        subject = deliveryResult.subject,
                        bodyPlainText = deliveryResult.body,
                        deliveryResult = deliveryResult,
                        triggeredBy = call.attributes.user.userId.toUUID(),
                    )
                }
            }

            call.respond(
                status = HttpStatusCode.OK,
                message = SendPartnershipEmailResponse(recipients = destinations.size),
            )
        }
    }
}
