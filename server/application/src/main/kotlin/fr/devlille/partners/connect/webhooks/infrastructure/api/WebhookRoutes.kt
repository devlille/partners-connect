package fr.devlille.partners.connect.webhooks.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.webhooks.domain.CreateEventWebhookRequest
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.webhookRoutes() {
    val repository by inject<WebhookRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/webhooks") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val request = call.receive<CreateEventWebhookRequest>()

            val webhookId = repository.createWebhook(eventSlug, request)
            call.respond(HttpStatusCode.Created, mapOf("id" to webhookId.toString()))
        }

        get {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val webhooks = repository.getWebhooks(eventSlug)
            call.respond(HttpStatusCode.OK, webhooks)
        }

        delete("/{webhookId}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val webhookId = call.parameters["webhookId"]?.toUUID()
                ?: throw BadRequestException("Missing or invalid webhook ID")

            repository.deleteWebhook(eventSlug, webhookId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
