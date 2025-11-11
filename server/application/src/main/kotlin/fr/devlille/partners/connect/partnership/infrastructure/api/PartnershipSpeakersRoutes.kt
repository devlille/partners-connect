package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.agenda.infrastructure.api.speakerId
import fr.devlille.partners.connect.partnership.domain.PartnershipSpeakerRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Configures routes for speaker-partnership attachment/detachment operations.
 * These are nested routes that should be called from within a route that already
 * has AuthorizedOrganisationPlugin installed.
 */
fun Route.publicPartnershipSpeakersRoutes() {
    val partnershipSpeakerRepository by inject<PartnershipSpeakerRepository>()

    route("/events/{eventSlug}/partnerships/{partnershipId}/speakers/{speakerId}") {
        post {
            val partnershipId = call.parameters.partnershipId
            val speakerId = call.parameters.speakerId
            val association = partnershipSpeakerRepository.attachSpeaker(partnershipId, speakerId)
            call.respond(HttpStatusCode.Created, association)
        }

        delete {
            val partnershipId = call.parameters.partnershipId
            val speakerId = call.parameters.speakerId
            partnershipSpeakerRepository.detachSpeaker(partnershipId, speakerId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
