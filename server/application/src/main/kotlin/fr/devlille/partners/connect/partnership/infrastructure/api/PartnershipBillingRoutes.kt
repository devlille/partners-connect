package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipBillingRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.publicPartnershipBillingRoutes() {
    val partnershipBillingRepository by inject<PartnershipBillingRepository>()
    val billingRepository by inject<BillingRepository>()
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventSlug}/partnerships/{partnershipId}/billing") {
        get {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val invoice = partnershipBillingRepository.getByPartnershipId(eventSlug, partnershipId)
            call.respond(HttpStatusCode.OK, invoice)
        }
        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val input = call.receive<CompanyBillingData>(schema = "company_billing_data.schema.json")
            val billingId = partnershipBillingRepository.createOrUpdate(eventSlug, partnershipId, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to billingId.toString()))
        }
        put {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val input = call.receive<CompanyBillingData>(schema = "company_billing_data.schema.json")
            val billingId = partnershipBillingRepository.createOrUpdate(eventSlug, partnershipId, input)
            call.respond(HttpStatusCode.OK, mapOf("id" to billingId.toString()))
        }
        post("invoice") {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val event = eventRepository.getBySlug(eventSlug)
            val partnership = partnershipRepository.getByIdDetailed(eventSlug, partnershipId)
            val invoiceUrl = billingRepository.createInvoice(event.event.id.toUUID(), partnership)
            partnershipBillingRepository.updateInvoiceUrl(eventSlug, partnershipId, invoiceUrl)
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val variables = NotificationVariables.NewInvoice(partnership.language, event, company, partnership)
            notificationRepository.sendMessage(eventSlug, variables)
            call.respond(HttpStatusCode.Created, mapOf("url" to invoiceUrl))
        }
        post("quote") {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val event = eventRepository.getBySlug(eventSlug)
            val partnership = partnershipRepository.getByIdDetailed(eventSlug, partnershipId)
            val quoteUrl = billingRepository.createQuote(event.event.id.toUUID(), partnership)
            partnershipBillingRepository.updateQuoteUrl(eventSlug, partnershipId, quoteUrl)
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val variables = NotificationVariables.NewQuote(partnership.language, event, company, partnership)
            notificationRepository.sendMessage(eventSlug, variables)
            call.respond(HttpStatusCode.Created, mapOf("url" to quoteUrl))
        }
    }
}

fun Route.orgsPartnershipBillingRoutes() {
    val partnershipBillingRepository by inject<PartnershipBillingRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/billing/{billingStatus}") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val status = call.parameters.billingStatus
            val id = partnershipBillingRepository.updateStatus(eventSlug, partnershipId, status)
            call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
        }
    }
}
