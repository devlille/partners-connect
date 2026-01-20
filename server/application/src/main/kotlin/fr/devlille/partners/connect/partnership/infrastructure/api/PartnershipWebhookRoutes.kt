package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.ktor.WebhookPartnershipPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.orgsPartnershipWebhookRoutes() {
    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/trigger/webhook") {
        install(WebhookPartnershipPlugin)

        post {
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
