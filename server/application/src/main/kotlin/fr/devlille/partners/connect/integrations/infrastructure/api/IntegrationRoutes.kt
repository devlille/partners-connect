package fr.devlille.partners.connect.integrations.infrastructure.api

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.integrationRoutes() {
    val integrationRepository by inject<IntegrationRepository>()
    val deserializerRegistry by inject<IntegrationDeserializerRegistry>()

    route("/orgs/{orgSlug}/events/{eventSlug}/integrations") {
        install(AuthorizedOrganisationPlugin)

        get {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Missing orgSlug")
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing eventSlug")
            val integrations = integrationRepository.findByEvent(orgSlug, eventSlug)
            call.respond(HttpStatusCode.OK, integrations)
        }

        post("/{provider}/{usage}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing eventSlug")
            val usageParam = call.parameters["usage"] ?: throw BadRequestException("Missing usage")
            val providerParam = call.parameters["provider"] ?: throw BadRequestException("Missing provider")
            val provider = runCatching { IntegrationProvider.valueOf(providerParam.uppercase()) }
                .getOrElse { throw BadRequestException("Invalid provider: $providerParam") }
            val usage = runCatching { IntegrationUsage.valueOf(usageParam.uppercase()) }
                .getOrElse { throw BadRequestException("Invalid usage: $usageParam") }
            val serializer = deserializerRegistry.serializerFor(provider)
            val json = Json { ignoreUnknownKeys = true }
            val input = json.decodeFromString(serializer, call.receiveText())
            val integrationId = integrationRepository.register(eventSlug, usage, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to integrationId.toString()))
        }

        delete("/{provider}/{usage}/{integrationId}") {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Missing orgSlug")
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing eventSlug")
            val usageParam = call.parameters["usage"] ?: throw BadRequestException("Missing usage")
            val usage = runCatching { IntegrationUsage.valueOf(usageParam.uppercase()) }
                .getOrElse { throw BadRequestException("Invalid usage: $usageParam") }
            val integrationId = call.parameters["integrationId"]?.toUUID()
                ?: throw BadRequestException("Missing integrationId")
            integrationRepository.deleteById(orgSlug, eventSlug, usage, integrationId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
