package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipAgreementRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.partnershipAgreementRoutes() {
    val eventRepository by inject<EventRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val agreementRepository by inject<PartnershipAgreementRepository>()
    val storageRepository by inject<PartnershipStorageRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/agreement") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val pdfBinary = agreementRepository.generateAgreement(eventSlug, partnershipId)
            val agreementUrl = storageRepository.uploadAgreement(eventSlug, partnershipId, pdfBinary)
            call.respond(HttpStatusCode.OK, mapOf("url" to agreementUrl))
        }
    }

    route("/events/{eventSlug}/partnerships/{partnershipId}/signed-agreement") {
        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val part = call.receiveMultipart().readPart() ?: throw MissingRequestParameterException("file")
            val bytes = part.asByteArray()
            if (part.contentType != ContentType.Application.Pdf) {
                throw UnsupportedMediaTypeException("Invalid file type, expected application/pdf")
            }
            val url = storageRepository.uploadSignedAgreement(eventSlug, partnershipId, bytes)
            agreementRepository.updateAgreementSignedUrl(eventSlug, partnershipId, url)
            val event = eventRepository.getBySlug(eventSlug)
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val variables = NotificationVariables.PartnershipAgreementSigned(partnership.language, event, company)
            notificationRepository.sendMessage(eventSlug, variables)
            call.respond(HttpStatusCode.OK, mapOf("url" to url))
        }
    }
}
