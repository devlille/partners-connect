package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerFilters
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerLifecycleEvent
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerNotificationRepository
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerRepository
import fr.devlille.partners.connect.ecosystempartners.domain.RegisterEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.domain.UpdateEcosystemPartner
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.WebhookEcosystemPartnerPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.partnership.infrastructure.api.toBooleanStrict
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import fr.devlille.partners.connect.webhooks.domain.WebhookResourceType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.ecosystemPartnerRoutes() {
    publicEcosystemPartnerRoutes()
    publicEcosystemPartnerListingRoute()
    orgsEcosystemPartnerRoutes()
}

private fun Route.publicEcosystemPartnerRoutes() {
    val repository by inject<EcosystemPartnerRepository>()
    val notificationRepository by inject<EcosystemPartnerNotificationRepository>()
    val webhookRepository by inject<WebhookRepository>()

    route("/events/{eventSlug}/ecosystem-partners") {
        post {
            val eventSlug = call.parameters.eventSlug
            val request = call.receive<RegisterEcosystemPartner>(
                schema = "register_ecosystem_partner.schema.json",
            )
            val id = repository.register(eventSlug, request)
            notificationRepository.notify(eventSlug, id, EcosystemPartnerLifecycleEvent.SUBMITTED)
            webhookRepository.sendWebhooks(
                eventSlug = eventSlug,
                resourceType = WebhookResourceType.ECOSYSTEM_PARTNER,
                resourceId = id,
                eventType = WebhookEventType.CREATED,
            )
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }
    }

    route("/events/{eventSlug}/ecosystem-partners/{ecosystemPartnerId}") {
        get {
            val eventSlug = call.parameters.eventSlug
            val id = call.parameters.ecosystemPartnerId
            call.respond(HttpStatusCode.OK, repository.getById(eventSlug, id))
        }
    }

    route("/events/{eventSlug}/ecosystem-partners/{ecosystemPartnerId}") {
        install(WebhookEcosystemPartnerPlugin)

        put {
            val eventSlug = call.parameters.eventSlug
            val id = call.parameters.ecosystemPartnerId
            val request = call.receive<UpdateEcosystemPartner>(
                schema = "update_ecosystem_partner.schema.json",
            )
            call.respond(HttpStatusCode.OK, repository.update(eventSlug, id, request))
        }
    }
}

private fun Route.publicEcosystemPartnerListingRoute() {
    val repository by inject<EcosystemPartnerRepository>()

    route("/events/{eventSlug}/ecosystem-partners") {
        get {
            val eventSlug = call.parameters.eventSlug
            call.respond(HttpStatusCode.OK, repository.listPublic(eventSlug))
        }
    }
}

private fun Route.orgsEcosystemPartnerRoutes() {
    val repository by inject<EcosystemPartnerRepository>()
    val notificationRepository by inject<EcosystemPartnerNotificationRepository>()
    val webhookRepository by inject<WebhookRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partners") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters.eventSlug
            val filters = EcosystemPartnerFilters(
                categoryId = call.request.queryParameters["filter[category_id]"],
                validated = call.request.queryParameters["filter[validated]"]?.toBoolean(),
                declined = call.request.queryParameters["filter[declined]"]
                    .toBooleanStrict("filter[declined]", default = false),
            )
            call.respond(HttpStatusCode.OK, repository.listByEvent(eventSlug, filters))
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partners/{ecosystemPartnerId}") {
        install(AuthorizedOrganisationPlugin)

        delete {
            val eventSlug = call.parameters.eventSlug
            val id = call.parameters.ecosystemPartnerId
            // Notify + fire webhook BEFORE deleting so the repository can still
            // read the partner row to build its payload.
            notificationRepository.notify(eventSlug, id, EcosystemPartnerLifecycleEvent.REMOVED)
            webhookRepository.sendWebhooks(
                eventSlug = eventSlug,
                resourceType = WebhookResourceType.ECOSYSTEM_PARTNER,
                resourceId = id,
                eventType = WebhookEventType.DELETED,
            )
            repository.delete(eventSlug, id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
