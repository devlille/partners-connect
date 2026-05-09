package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventStorageRepository
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.partnership.domain.PartnershipBoothRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.eventBoothPlanRoutes() {
    val eventRepository by inject<EventRepository>()
    val storageRepository by inject<EventStorageRepository>()
    val boothRepository by inject<PartnershipBoothRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/booth-plan") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters.eventSlug
            call.respond(HttpStatusCode.OK, boothRepository.listBoothLocations(eventSlug))
        }

        post {
            val eventSlug = call.parameters.eventSlug
            val part = call.receiveMultipart().readPart() ?: throw MissingRequestParameterException("file")
            val bytes = part.asByteArray()
            val contentType = part.contentType?.toString()?.lowercase()
                ?: throw UnsupportedMediaTypeException("Content type not supported")

            val imageUrl = storageRepository.uploadBoothPlanImage(eventSlug, bytes, contentType)
            eventRepository.updateBoothPlanImageUrl(eventSlug, imageUrl)
            call.respond(HttpStatusCode.Created, mapOf("url" to imageUrl))
        }
    }
}
