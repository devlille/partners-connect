package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerDecisionRepository
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerLifecycleEvent
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.NotificationEcosystemPartnerPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.WebhookEcosystemPartnerPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.ecosystemPartnerLifecycle
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.ecosystemPartnerDecisionRoutes() {
    val repository by inject<EcosystemPartnerDecisionRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partners/{ecosystemPartnerId}/validate") {
        install(AuthorizedOrganisationPlugin)
        install(NotificationEcosystemPartnerPlugin)
        install(WebhookEcosystemPartnerPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val id = call.parameters.ecosystemPartnerId
            val result = repository.validate(eventSlug, id)
            call.attributes.ecosystemPartnerLifecycle = EcosystemPartnerLifecycleEvent.VALIDATED
            call.respond(HttpStatusCode.OK, mapOf("id" to result.toString()))
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partners/{ecosystemPartnerId}/decline") {
        install(AuthorizedOrganisationPlugin)
        install(NotificationEcosystemPartnerPlugin)
        install(WebhookEcosystemPartnerPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val id = call.parameters.ecosystemPartnerId
            val result = repository.decline(eventSlug, id)
            call.attributes.ecosystemPartnerLifecycle = EcosystemPartnerLifecycleEvent.DECLINED
            call.respond(HttpStatusCode.OK, mapOf("id" to result.toString()))
        }
    }
}
