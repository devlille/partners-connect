package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import fr.devlille.partners.connect.partnership.domain.SupportVideoRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

private val SUPPORTED_VIDEO_TYPES = setOf("video/mp4", "video/webm")

fun Route.publicPartnershipSupportVideoRoutes() {
    val storageRepository by inject<PartnershipStorageRepository>()
    val repository by inject<SupportVideoRepository>()

    route("/events/{eventSlug}/partnerships/{partnershipId}/support-video") {
        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId

            val multipart = call.receiveMultipart()
            val part = multipart.readPart() ?: throw MissingRequestParameterException("file")
            val mimeType = part.contentType?.toString()
                ?: throw UnsupportedMediaTypeException("Missing content type on uploaded file")
            if (mimeType !in SUPPORTED_VIDEO_TYPES) {
                throw UnsupportedMediaTypeException("Unsupported video type: $mimeType")
            }
            val bytes = part.asByteArray()

            repository.preCheckSubmission(eventSlug, partnershipId)
            val url = storageRepository.uploadSupportVideo(eventSlug, partnershipId, bytes, mimeType)
            val id = repository.submit(eventSlug, partnershipId, url)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }
        get {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            call.respond(HttpStatusCode.OK, repository.get(eventSlug, partnershipId))
        }
    }
}
