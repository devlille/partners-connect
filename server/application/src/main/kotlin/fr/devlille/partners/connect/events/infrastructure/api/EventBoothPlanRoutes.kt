package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.events.domain.EventStorageRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
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
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")

            val multipart = call.receiveMultipart()
            val part = multipart.readPart() ?: throw BadRequestException("Missing file part")
            val bytes = part.asByteArray()

            // Validate that it's an image MIME type
            val contentType = part.contentType?.toString()?.lowercase()
            if (contentType == null || !contentType.startsWith("image/")) {
                throw BadRequestException("Invalid file type, expected an image")
            }

            // Validate specific supported image types
            if (part.contentType !in listOf(ContentType.Image.PNG, ContentType.Image.JPEG, ContentType.Image.GIF)) {
                throw BadRequestException("Unsupported image type: ${part.contentType}")
            }

            val imageUrl = storageRepository.uploadBoothPlanImage(eventSlug, bytes, contentType)
            eventRepository.updateBoothPlanImageUrl(eventSlug, imageUrl)

            call.respond(HttpStatusCode.OK, mapOf("url" to imageUrl))
        }
    }
}
