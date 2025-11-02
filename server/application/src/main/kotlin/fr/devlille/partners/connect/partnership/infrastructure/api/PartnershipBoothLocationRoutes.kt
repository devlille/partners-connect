package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.EmptyStringValidationException
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.partnership.domain.PartnershipBoothRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class BoothLocationRequest(
    val location: String,
)

@Serializable
data class BoothLocationResponse(
    val id: String,
    val location: String,
)

fun Route.orgsPartnershipBoothLocationRoutes() {
    val repository by inject<PartnershipBoothRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/booth-location") {
        install(AuthorizedOrganisationPlugin)

        put {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val request = call.receive<BoothLocationRequest>(schema = "booth_location_request.schema.json")
            val location = request.location.trim()
            if (location.isBlank()) {
                throw EmptyStringValidationException("location")
            }
            repository.updateBoothLocation(eventSlug, partnershipId, location)
            call.respond(
                status = HttpStatusCode.OK,
                message = BoothLocationResponse(id = partnershipId.toString(), location = location),
            )
        }
    }
}
