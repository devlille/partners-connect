package fr.devlille.partners.connect.integrations.infrastructure.api

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
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

@Suppress("ThrowsCount", "LongMethod")
fun Route.integrationRoutes() {
    val integrationRepository by inject<IntegrationRepository>()
    val deserializerRegistry by inject<IntegrationDeserializerRegistry>()

    route("/orgs/{orgSlug}/events/{eventSlug}/integrations") {
        install(AuthorizedOrganisationPlugin)

        get {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException(
                code = ErrorCode.BAD_REQUEST,
                message = "Missing orgSlug",
            )
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                code = ErrorCode.BAD_REQUEST,
                message = "Missing eventSlug",
            )
            val integrations = integrationRepository.findByEvent(orgSlug, eventSlug)
            call.respond(HttpStatusCode.OK, integrations)
        }

        post("/{provider}/{usage}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                code = ErrorCode.BAD_REQUEST,
                message = "Missing eventSlug",
            )
            val usageParam = call.parameters["usage"] ?: throw BadRequestException(
                code = ErrorCode.BAD_REQUEST,
                message = "Missing usage",
            )
            val providerParam = call.parameters["provider"] ?: throw BadRequestException(
                code = ErrorCode.BAD_REQUEST,
                message = "Missing provider",
            )
            val provider = runCatching { IntegrationProvider.valueOf(providerParam.uppercase()) }
                .getOrElse {
                    throw BadRequestException(
                        code = ErrorCode.BAD_REQUEST,
                        message = "Invalid provider: $providerParam",
                    )
                }
            val usage = runCatching { IntegrationUsage.valueOf(usageParam.uppercase()) }
                .getOrElse {
                    throw BadRequestException(
                        code = ErrorCode.BAD_REQUEST,
                        message = "Invalid usage: $usageParam",
                    )
                }
            val serializer = deserializerRegistry.serializerFor(provider)
            val json = Json { ignoreUnknownKeys = true }
            val input = json.decodeFromString(serializer, call.receiveText())
            val integrationId = integrationRepository.register(eventSlug, usage, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to integrationId.toString()))
        }

        delete("/{integrationId}") {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException(
                code = ErrorCode.BAD_REQUEST,
                message = "Missing orgSlug",
            )
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                code = ErrorCode.BAD_REQUEST,
                message = "Missing eventSlug",
            )
            val integrationId = call.parameters["integrationId"]?.toUUID()
                ?: throw BadRequestException(
                    code = ErrorCode.BAD_REQUEST,
                    message = "Missing integrationId",
                )
            integrationRepository.deleteById(orgSlug, eventSlug, integrationId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
