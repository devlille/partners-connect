package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyInvoice
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedEventPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.invoices.domain.InvoiceRepository
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipInvoiceRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
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
fun Route.partnershipRoutes() {
    val eventRepository by inject<EventRepository>()
    val packRepository by inject<PackRepository>()
    val companyRepository by inject<CompanyRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventId}/companies/{companyId}/partnership") {
        post {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val companyId = call.parameters["companyId"]?.toUUID() ?: throw BadRequestException("Missing company id")
            val register = call.receive<RegisterPartnership>()
            val id = partnershipRepository.register(eventId, companyId, register)
            val company = companyRepository.getById(companyId)
            val pack = packRepository.getById(eventId, register.packId.toUUID(), register.language)
            val event = eventRepository.getById(eventId)
            val variables = NotificationVariables.NewPartnership(register.language, event, company, pack)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

        route("/{partnershipId}/validate") {
            install(AuthorizedEventPlugin)

            post {
                val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
                val companyId = call.parameters["companyId"]?.toUUID()
                    ?: throw BadRequestException("Missing company id")
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException("Missing partnership id")
                val id = partnershipRepository.validate(eventId, partnershipId)
                val company = companyRepository.getById(companyId)
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
                val companyId = call.parameters["companyId"]?.toUUID()
                    ?: throw BadRequestException("Missing company id")
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException("Missing partnership id")
                val id = partnershipRepository.decline(eventId, partnershipId)
                val company = companyRepository.getById(companyId)
                val partnership = partnershipRepository.getById(eventId, partnershipId)
                val event = eventRepository.getById(eventId)
                val variables = NotificationVariables.PartnershipDeclined(partnership.language, event, company)
                notificationRepository.sendMessage(eventId, variables)
                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }
    }
}

@Suppress("ThrowsCount")
fun Route.partnershipSuggestionRoutes() {
    val eventRepository by inject<EventRepository>()
    val packRepository by inject<PackRepository>()
    val companyRepository by inject<CompanyRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val suggestionRepository by inject<PartnershipSuggestionRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventId}/companies/{companyId}/partnership/{partnershipId}") {
        route("/suggestion") {
            install(AuthorizedEventPlugin)
            post {
                val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
                val companyId = call.parameters["companyId"]?.toUUID()
                    ?: throw BadRequestException("Missing company id")
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException("Missing partnership id")
                val input = call.receive<SuggestPartnership>()
                val id = suggestionRepository.suggest(eventId, companyId, partnershipId, input)
                val pack = packRepository.getById(eventId, input.packId.toUUID(), input.language)
                val company = companyRepository.getById(companyId)
                val event = eventRepository.getById(eventId)
                val variables = NotificationVariables.NewSuggestion(input.language, event, company, pack)
                notificationRepository.sendMessage(eventId, variables)
                call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
            }
        }
        post("/suggestion-approve") {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val companyId = call.parameters["companyId"]?.toUUID()
                ?: throw BadRequestException("Missing company id")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val id = suggestionRepository.approve(eventId, companyId, partnershipId)
            val company = companyRepository.getById(companyId)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            val event = eventRepository.getById(eventId)
            val variables = NotificationVariables.SuggestionApproved(partnership.language, event, company)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }

        post("/suggestion-decline") {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val companyId = call.parameters["companyId"]?.toUUID()
                ?: throw BadRequestException("Missing company id")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val id = suggestionRepository.decline(eventId, companyId, partnershipId)
            val company = companyRepository.getById(companyId)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            val event = eventRepository.getById(eventId)
            val variables = NotificationVariables.SuggestionDeclined(partnership.language, event, company)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
}

@Suppress("ThrowsCount")
fun Route.partnershipInvoiceRoutes() {
    val partnershipInvoiceRepository by inject<PartnershipInvoiceRepository>()
    val invoiceRepository by inject<InvoiceRepository>()
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val companyRepository by inject<CompanyRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventId}/companies/{companyId}/partnership/{partnershipId}/invoice") {
        get {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val companyId = call.parameters["companyId"]?.toUUID() ?: throw BadRequestException("Missing company id")
            val invoice = partnershipInvoiceRepository.getByCompanyId(eventId, companyId)
            call.respond(HttpStatusCode.OK, invoice)
        }
        post {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val companyId = call.parameters["companyId"]?.toUUID() ?: throw BadRequestException("Missing company id")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val input = call.receive<CompanyInvoice>()
            val invoiceId = partnershipInvoiceRepository.createOrUpdate(eventId, companyId, input)
            val invoiceUrl = invoiceRepository.createInvoice(eventId, companyId)
            partnershipInvoiceRepository.updateInvoiceUrl(eventId, companyId, invoiceUrl)
            val event = eventRepository.getById(eventId)
            val company = companyRepository.getById(companyId)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            val variables = NotificationVariables.NewInvoice(partnership.language, event, company)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to invoiceId.toString(), "invoiceUrl" to invoiceUrl))
        }
        put {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val companyId = call.parameters["companyId"]?.toUUID() ?: throw BadRequestException("Missing company id")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val input = call.receive<CompanyInvoice>()
            val invoiceId = partnershipInvoiceRepository.createOrUpdate(eventId, companyId, input)
            val invoiceUrl = invoiceRepository.createInvoice(eventId, companyId)
            partnershipInvoiceRepository.updateInvoiceUrl(eventId, companyId, invoiceUrl)
            val event = eventRepository.getById(eventId)
            val company = companyRepository.getById(companyId)
            val partnership = partnershipRepository.getById(eventId, partnershipId)
            val variables = NotificationVariables.NewInvoice(partnership.language, event, company)
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to invoiceId.toString(), "invoiceUrl" to invoiceUrl))
        }
    }
}
