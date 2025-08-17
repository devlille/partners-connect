package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipBillingRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

@Suppress("ThrowsCount", "LongMethod")
fun Route.partnershipBillingRoutes() {
    val partnershipBillingRepository by inject<PartnershipBillingRepository>()
    val billingRepository by inject<BillingRepository>()
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()
    route("/events/{eventSlug}/partnership/{partnershipId}/billing") {
        get {
            val eventId = eventRepository.getIdBySlug(
                call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug"),
            )
            call.respond(
                HttpStatusCode.OK,
                partnershipBillingRepository.getByPartnershipId(
                    eventId,
                    call.parameters["partnershipId"]?.toUUID()
                        ?: throw BadRequestException("Missing partnership id"),
                ),
            )
        }
        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val eventId = eventRepository.getIdBySlug(eventSlug)
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val input = call.receive<CompanyBillingData>()
            val billingId = partnershipBillingRepository.createOrUpdate(eventId, partnershipId, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to billingId.toString()))
        }
        put {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val eventId = eventRepository.getIdBySlug(eventSlug)
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val input = call.receive<CompanyBillingData>()
            val billingId = partnershipBillingRepository.createOrUpdate(eventId, partnershipId, input)
            call.respond(HttpStatusCode.OK, mapOf("id" to billingId.toString()))
        }
        post("invoice") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val eventId = eventRepository.getIdBySlug(eventSlug)
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val invoiceUrl = billingRepository.createInvoice(eventId, partnershipId)
            partnershipBillingRepository.updateInvoiceUrl(eventId, partnershipId, invoiceUrl)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            notificationRepository.sendMessage(
                eventId,
                NotificationVariables.NewInvoice(
                    partnership.language,
                    eventRepository.getById(eventId),
                    partnershipRepository.getCompanyByPartnershipId(eventId, partnershipId),
                ),
            )
            call.respond(HttpStatusCode.Created, mapOf("url" to invoiceUrl))
        }
        post("quote") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val eventId = eventRepository.getIdBySlug(eventSlug)
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val quoteUrl = billingRepository.createQuote(eventId, partnershipId)
            partnershipBillingRepository.updateQuoteUrl(eventId, partnershipId, quoteUrl)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            notificationRepository.sendMessage(
                eventId,
                NotificationVariables.NewInvoice(
                    partnership.language,
                    eventRepository.getById(eventId),
                    partnershipRepository.getCompanyByPartnershipId(eventId, partnershipId),
                ),
            )
            call.respond(HttpStatusCode.Created, mapOf("url" to quoteUrl))
        }
    }
}
