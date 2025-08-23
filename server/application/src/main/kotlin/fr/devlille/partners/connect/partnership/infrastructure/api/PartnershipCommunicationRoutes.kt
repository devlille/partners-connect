package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import kotlin.getValue

@Suppress("ThrowsCount", "LongMethod")
fun Route.partnershipCommunicationRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()
    val storageRepository by inject<PartnershipStorageRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/communication") {
        install(AuthorizedOrganisationPlugin)

        route("/publication") {
            put {
                val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                    code = ErrorCode.BAD_REQUEST,
                    message = "Missing event slug",
                )
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException(
                        code = ErrorCode.BAD_REQUEST,
                        message = "Missing partnership id",
                    )

                val requestBody = call.receive<PublicationDateRequest>()
                val publicationDate = requestBody.publicationDate

                val id = partnershipRepository.updateCommunicationPublicationDate(
                    eventSlug,
                    partnershipId,
                    publicationDate,
                )

                call.respond(
                    HttpStatusCode.OK,
                    PublicationDateResponse(
                        id = id.toString(),
                        publicationDate = publicationDate,
                    ),
                )
            }
        }

        route("/support") {
            put {
                val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                    code = ErrorCode.BAD_REQUEST,
                    message = "Missing event slug",
                )
                val partnershipId = call.parameters["partnershipId"]?.toUUID()
                    ?: throw BadRequestException(
                        code = ErrorCode.BAD_REQUEST,
                        message = "Missing partnership id",
                    )

                val contentType = call.request.contentType()
                val bytes = call.receive<ByteArray>()

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

                call.respond(
                    HttpStatusCode.OK,
                    SupportUploadResponse(
                        id = id.toString(),
                        url = supportUrl,
                    ),
                )
            }
        }
    }
}

@Serializable
data class PublicationDateRequest(
    @SerialName("publication_date")
    val publicationDate: LocalDateTime,
)

@Serializable
data class PublicationDateResponse(
    val id: String,
    @SerialName("publication_date")
    val publicationDate: LocalDateTime,
)

@Serializable
data class SupportUploadResponse(
    val id: String,
    val url: String,
)
