package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyInvoice
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.invoices.domain.InvoiceRepository
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipInvoiceRepository
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

@Suppress("ThrowsCount")
fun Route.partnershipInvoiceRoutes() {
    val partnershipInvoiceRepository by inject<PartnershipInvoiceRepository>()
    val invoiceRepository by inject<InvoiceRepository>()
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventId}/partnership/{partnershipId}/invoice") {
        get {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val invoice = partnershipInvoiceRepository.getByPartnershipId(eventId, partnershipId)
            call.respond(HttpStatusCode.OK, invoice)
        }
        post {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val input = call.receive<CompanyInvoice>()
            val invoiceId = partnershipInvoiceRepository.createOrUpdate(eventId, partnershipId, input)
            val invoiceUrl = invoiceRepository.createInvoice(eventId, partnershipId)
            partnershipInvoiceRepository.updateInvoiceUrl(eventId, partnershipId, invoiceUrl)
            val event = eventRepository.getById(eventId)
            val company = partnershipRepository.getCompanyByPartnershipId(eventId, partnershipId)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            val variables = NotificationVariables.NewInvoice(partnership.language, event, company)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to invoiceId.toString(), "invoiceUrl" to invoiceUrl))
        }
        put {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val input = call.receive<CompanyInvoice>()
            val invoiceId = partnershipInvoiceRepository.createOrUpdate(eventId, partnershipId, input)
            val invoiceUrl = invoiceRepository.createInvoice(eventId, partnershipId)
            partnershipInvoiceRepository.updateInvoiceUrl(eventId, partnershipId, invoiceUrl)
            val event = eventRepository.getById(eventId)
            val company = partnershipRepository.getCompanyByPartnershipId(eventId, partnershipId)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            val variables = NotificationVariables.NewInvoice(partnership.language, event, company)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to invoiceId.toString(), "invoiceUrl" to invoiceUrl))
        }
    }
}
