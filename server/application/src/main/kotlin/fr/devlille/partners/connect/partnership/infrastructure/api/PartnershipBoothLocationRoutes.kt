package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import kotlin.getValue

@Serializable
data class BoothLocationRequest(
    val location: String,
)

@Serializable
data class BoothLocationResponse(
    val id: String,
    val location: String,
)

@Suppress("ThrowsCount")
fun Route.partnershipBoothLocationRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/booth-location") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")

            val request = call.receive<BoothLocationRequest>()
            val location = request.location.trim()

            if (location.isBlank()) {
                throw BadRequestException("Location cannot be empty")
            }

            // Check if location is already taken by another partnership for this event
            if (partnershipRepository.isBoothLocationTaken(eventSlug, location, excludePartnershipId = partnershipId)) {
                throw ForbiddenException(
                    "Location '$location' is already assigned to another partnership for this event",
                )
            }

            val partnership = partnershipRepository.updateBoothLocation(eventSlug, partnershipId, location)

            call.respond(
                HttpStatusCode.OK,
                BoothLocationResponse(
                    id = partnership.id,
                    location = location,
                ),
            )
        }
    }
}
