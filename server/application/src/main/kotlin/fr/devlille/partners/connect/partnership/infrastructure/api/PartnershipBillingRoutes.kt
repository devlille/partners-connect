package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipBillingRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

@Suppress("ThrowsCount", "LongMethod", "CyclomaticComplexMethod")
fun Route.partnershipBillingRoutes() {
    val partnershipBillingRepository by inject<PartnershipBillingRepository>()
    val billingRepository by inject<BillingRepository>()
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventSlug}/partnership/{partnershipId}/billing") {
        get {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException(
                    message = "Missing partnership id",
                )
            val invoice = partnershipBillingRepository.getByPartnershipId(eventSlug, partnershipId)
            call.respond(HttpStatusCode.OK, invoice)
        }
        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException(
                    message = "Missing partnership id",
                )
            val input = call.receive<CompanyBillingData>()
            val billingId = partnershipBillingRepository.createOrUpdate(eventSlug, partnershipId, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to billingId.toString()))
        }
        put {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException(
                    message = "Missing partnership id",
                )
            val input = call.receive<CompanyBillingData>()
            val billingId = partnershipBillingRepository.createOrUpdate(eventSlug, partnershipId, input)
            call.respond(HttpStatusCode.OK, mapOf("id" to billingId.toString()))
        }
        post("invoice") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException(
                    message = "Missing partnership id",
                )
            val invoiceUrl = billingRepository.createInvoice(eventSlug, partnershipId)
            partnershipBillingRepository.updateInvoiceUrl(eventSlug, partnershipId, invoiceUrl)
            val event = eventRepository.getBySlug(eventSlug)
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val variables = NotificationVariables.NewInvoice(partnership.language, event, company)
            notificationRepository.sendMessage(eventSlug, variables)
            call.respond(HttpStatusCode.Created, mapOf("url" to invoiceUrl))
        }
        post("quote") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException(
                    message = "Missing partnership id",
                )
            val quoteUrl = billingRepository.createQuote(eventSlug, partnershipId)
            partnershipBillingRepository.updateQuoteUrl(eventSlug, partnershipId, quoteUrl)
            val event = eventRepository.getBySlug(eventSlug)
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val variables = NotificationVariables.NewInvoice(partnership.language, event, company)
            notificationRepository.sendMessage(eventSlug, variables)
            call.respond(HttpStatusCode.Created, mapOf("url" to quoteUrl))
        }
    }

    organizationProtectedBillingRoutes()
}

@Suppress("ThrowsCount")
private fun Route.organizationProtectedBillingRoutes() {
    val partnershipBillingRepository by inject<PartnershipBillingRepository>()

    // Organization-protected routes for organizers
    route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/billing") {
        install(AuthorizedOrganisationPlugin)

        post("/{billingStatus}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException(
                    message = "Missing partnership id",
                )
            val statusParam = call.parameters["billingStatus"] ?: throw BadRequestException(
                message = "Missing billing status",
            )
            val status = runCatching { InvoiceStatus.valueOf(statusParam.uppercase()) }
                .getOrElse {
                    throw BadRequestException(
                        message = "Invalid billing status: $statusParam",
                    )
                }

            val id = partnershipBillingRepository.updateStatus(eventSlug, partnershipId, status)
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
}
