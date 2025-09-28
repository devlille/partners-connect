package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import io.ktor.http.HttpStatusCode
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

fun Route.partnershipCommunicationRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()
    val storageRepository by inject<PartnershipStorageRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/communication") {
        install(AuthorizedOrganisationPlugin)

        route("/publication") {
            put {
                val eventSlug = call.parameters.eventSlug
                val partnershipId = call.parameters.partnershipId
                val publicationDate = call
                    .receive<PublicationDateRequest>(schema = "publication_date_request.schema.json")
                    .publicationDate
                val id = partnershipRepository
                    .updateCommunicationPublicationDate(eventSlug, partnershipId, publicationDate)
                call.respond(
                    status = HttpStatusCode.OK,
                    message = PublicationDateResponse(id = id.toString(), publicationDate = publicationDate),
                )
            }
        }

        route("/support") {
            put {
                val eventSlug = call.parameters.eventSlug
                val partnershipId = call.parameters.partnershipId
                val contentType = call.request.contentType()
                val bytes = call.receive<ByteArray>()
                val supportUrl = storageRepository
                    .uploadCommunicationSupport(eventSlug, partnershipId, bytes, contentType.toString())
                val id = partnershipRepository
                    .updateCommunicationSupportUrl(eventSlug, partnershipId, supportUrl)
                call.respond(
                    status = HttpStatusCode.OK,
                    message = SupportUploadResponse(id = id.toString(), url = supportUrl),
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
