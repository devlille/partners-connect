package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.application.EventRepositoryExposed
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipAgreementRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

@Suppress("ThrowsCount")
fun Route.partnershipAgreementRoutes() {
    val eventRepository by inject<EventRepositoryExposed>()
    val partnershipRepository by inject<PartnershipRepository>()
    val agreementRepository by inject<PartnershipAgreementRepository>()
    val storageRepository by inject<PartnershipStorageRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/agreement") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val pdfBinary = agreementRepository.generateAgreement(eventSlug, partnershipId)
            val agreementUrl = storageRepository.uploadAgreement(eventSlug, partnershipId, pdfBinary)
            call.respond(HttpStatusCode.OK, mapOf("url" to agreementUrl))
        }
    }

    route("/events/{eventSlug}/partnership/{partnershipId}/signed-agreement") {
        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing event slug")
            val partnershipId = call.parameters["partnershipId"]?.toUUID()
                ?: throw BadRequestException("Missing partnership id")
            val multipart = call.receiveMultipart()
            val part = multipart.readPart() ?: throw BadRequestException("Missing file part")
            val bytes = part.asByteArray()
            if (part.contentType != ContentType.Application.Pdf) {
                throw BadRequestException("Invalid file type, expected application/pdf")
            }
            val url = storageRepository.uploadSignedAgreement(eventSlug, partnershipId, bytes)
            agreementRepository.updateAgreementSignedUrl(eventSlug, partnershipId, url)
            val event = eventRepository.getBySlug(eventSlug).event
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val variables = NotificationVariables.PartnershipAgreementSigned(partnership.language, event, company)
            // Note: we need eventId for notification, get it from event  
            val eventId = EventEntity.findBySlug(eventSlug)?.id?.value
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            notificationRepository.sendMessage(eventId, variables)
            call.respond(HttpStatusCode.OK, mapOf("url" to url))
        }
    }
}
