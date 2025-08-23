package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventStorageRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

@Suppress("ThrowsCount")
fun Route.eventBoothPlanRoutes() {
    val eventRepository by inject<EventRepository>()
    val storageRepository by inject<EventStorageRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/booth-plan") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )

            val multipart = call.receiveMultipart()
            val part = multipart.readPart() ?: throw BadRequestException(
                message = "Missing file part",
            )
            val bytes = part.asByteArray()
            val contentType = part.contentType?.toString()?.lowercase()
                ?: throw BadRequestException(
                    message = "Content type is required",
                )

            val imageUrl = storageRepository.uploadBoothPlanImage(eventSlug, bytes, contentType)
            eventRepository.updateBoothPlanImageUrl(eventSlug, imageUrl)

            call.respond(HttpStatusCode.Created, mapOf("url" to imageUrl))
        }
    }
}
