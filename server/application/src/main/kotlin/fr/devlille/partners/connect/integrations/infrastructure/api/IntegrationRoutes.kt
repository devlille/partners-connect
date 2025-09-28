package fr.devlille.partners.connect.integrations.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.organisations.infrastructure.api.orgSlug
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Route.integrationRoutes() {
    val integrationRepository by inject<IntegrationRepository>()
    val deserializerRegistry by inject<IntegrationDeserializerRegistry>()

    route("/orgs/{orgSlug}/events/{eventSlug}/integrations") {
        install(AuthorizedOrganisationPlugin)

        get {
            val orgSlug = call.parameters.orgSlug
            val eventSlug = call.parameters.eventSlug
            val integrations = integrationRepository.findByEvent(orgSlug, eventSlug)
            call.respond(HttpStatusCode.OK, integrations)
        }

        post("/{provider}/{usage}") {
            val eventSlug = call.parameters.eventSlug
            val usage = call.parameters.usage
            val provider = call.parameters.provider
            val serializer = deserializerRegistry.serializerFor(provider)
            val json = Json { ignoreUnknownKeys = true }
            val input = json.decodeFromString(serializer, call.receiveText())
            val integrationId = integrationRepository.register(eventSlug, usage, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to integrationId.toString()))
        }

        delete("/{provider}/{usage}/{integrationId}") {
            val orgSlug = call.parameters.orgSlug
            val eventSlug = call.parameters.eventSlug
            val usage = call.parameters.usage
            val integrationId = call.parameters.integrationId
            integrationRepository.deleteById(orgSlug, eventSlug, usage, integrationId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
