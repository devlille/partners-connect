package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerDecisionRepository
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerRepository
import fr.devlille.partners.connect.ecosystempartners.domain.publicEventUrl
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.WebhookEcosystemPartnerPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.ecosystemPartnerDecisionRoutes() {
    val repository by inject<EcosystemPartnerDecisionRepository>()
    val ecosystemPartnerRepository by inject<EcosystemPartnerRepository>()
    val eventRepository by inject<EventRepository>()
    val companyRepository by inject<CompanyRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partners/{ecosystemPartnerId}/validate") {
        install(AuthorizedOrganisationPlugin)
        install(WebhookEcosystemPartnerPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val id = call.parameters.ecosystemPartnerId
            val result = repository.validate(eventSlug, id)
            val partner = ecosystemPartnerRepository.getById(eventSlug, id)
            val event = eventRepository.getBySlug(eventSlug)
            val company = companyRepository.getById(partner.companyId.toUUID())
            val variables = NotificationVariables.EcosystemPartnerValidated(
                language = partner.language,
                event = event,
                company = company,
                categoryName = partner.category.name,
                publicEventUrl = publicEventUrl(event),
            )
            notificationRepository.sendMessage(variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to result.toString()))
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partners/{ecosystemPartnerId}/decline") {
        install(AuthorizedOrganisationPlugin)
        install(WebhookEcosystemPartnerPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val id = call.parameters.ecosystemPartnerId
            val result = repository.decline(eventSlug, id)
            val partner = ecosystemPartnerRepository.getById(eventSlug, id)
            val event = eventRepository.getBySlug(eventSlug)
            val company = companyRepository.getById(partner.companyId.toUUID())
            val variables = NotificationVariables.EcosystemPartnerDeclined(
                language = partner.language,
                event = event,
                company = company,
                categoryName = partner.category.name,
            )
            notificationRepository.sendMessage(variables)
            call.respond(HttpStatusCode.OK, mapOf("id" to result.toString()))
        }
    }
}
