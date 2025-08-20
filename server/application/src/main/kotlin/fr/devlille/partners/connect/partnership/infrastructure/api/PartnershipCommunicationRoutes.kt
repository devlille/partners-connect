package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import kotlin.getValue

@Suppress("ThrowsCount")
fun Route.partnershipCommunicationRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()
    val storageRepository by inject<PartnershipStorageRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/communication") {
        install(AuthorizedOrganisationPlugin)

        route("/publication") {
            post {
                call.handlePublicationDateRequest(partnershipRepository)
            }
        }

        route("/support") {
            post {
                call.handleSupportUploadRequest(partnershipRepository, storageRepository)
            }
        }
    }
}

@Suppress("ThrowsCount")
private suspend fun io.ktor.server.application.ApplicationCall.handlePublicationDateRequest(
    partnershipRepository: PartnershipRepository,
) {
    val eventSlug = parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
    val partnershipId = parameters["partnershipId"]?.toUUID()
        ?: throw BadRequestException("Missing partnership id")

    val requestBody = receive<PublicationDateRequest>()

    // Parse the ISO-8601 datetime string
    val publicationDate = try {
        LocalDateTime.parse(requestBody.publicationDate)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid publication date format. Expected ISO-8601 datetime string.", e)
    }

    val id = partnershipRepository.updateCommunicationPublicationDate(
        eventSlug,
        partnershipId,
        publicationDate,
    )

    respond(
        HttpStatusCode.OK,
        PublicationDateResponse(
            id = id.toString(),
            publicationDate = requestBody.publicationDate,
        ),
    )
}

@Suppress("ThrowsCount", "SwallowedException")
private suspend fun io.ktor.server.application.ApplicationCall.handleSupportUploadRequest(
    partnershipRepository: PartnershipRepository,
    storageRepository: PartnershipStorageRepository,
) {
    val eventSlug = parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
    val partnershipId = parameters["partnershipId"]?.toUUID()
        ?: throw BadRequestException("Missing partnership id")

    val contentType = request.contentType()
    if (!isValidImageType(contentType)) {
        throw BadRequestException("Invalid file type. Expected image (PNG, JPEG, GIF, SVG, WebP)")
    }

    val bytes = receive<ByteArray>()

    if (bytes.isEmpty()) {
        throw BadRequestException("Empty file content")
    }

    val supportUrl = storageRepository.uploadCommunicationSupport(
        eventSlug,
        partnershipId,
        bytes,
        contentType.toString(),
    )

    val id = partnershipRepository.updateCommunicationSupportUrl(
        eventSlug,
        partnershipId,
        supportUrl,
    )

    respond(
        HttpStatusCode.OK,
        SupportUploadResponse(
            id = id.toString(),
            supportUrl = supportUrl,
        ),
    )
}

@Serializable
data class PublicationDateRequest(
    @SerialName("publication_date")
    val publicationDate: String,
)

@Serializable
data class PublicationDateResponse(
    val id: String,
    @SerialName("publication_date")
    val publicationDate: String,
)

@Serializable
data class SupportUploadResponse(
    val id: String,
    @SerialName("support_url")
    val supportUrl: String,
)

private fun isValidImageType(contentType: ContentType?): Boolean {
    return when (contentType) {
        ContentType.Image.PNG,
        ContentType.Image.JPEG,
        ContentType.Image.GIF,
        ContentType.Image.SVG,
        -> true
        else -> contentType?.toString() == "image/webp"
    }
}
