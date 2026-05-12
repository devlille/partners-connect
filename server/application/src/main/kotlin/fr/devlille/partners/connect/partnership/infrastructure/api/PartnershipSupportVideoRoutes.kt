package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.NotificationPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.WebhookPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.ktor.variables
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.organisations.infrastructure.api.orgSlug
import fr.devlille.partners.connect.partnership.domain.DeclineSupportVideoRequest
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSupportVideoRepository
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

@Suppress("ThrowsCount")
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

fun Route.orgsPartnershipSupportVideoRoutes() {
    val repository by inject<PartnershipSupportVideoRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/support-videos") {
        install(AuthorizedOrganisationPlugin)

        get {
            val orgSlug = call.parameters.orgSlug
            val eventSlug = call.parameters.eventSlug
            val statusParam = call.request.queryParameters["status"]
            val status = statusParam?.let { PromotionStatus.valueOf(it.uppercase()) }
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            call.respond(
                HttpStatusCode.OK,
                repository.listEventSupportVideos(orgSlug, eventSlug, status, page, pageSize),
            )
        }
    }
}

@Suppress("LongMethod")
fun Route.orgsPartnershipSupportVideoDecisionRoutes() {
    val repository by inject<PartnershipSupportVideoRepository>()
    val authRepository by inject<AuthRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val eventRepository by inject<EventRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/support-videos/{videoId}/approve") {
        install(AuthorizedOrganisationPlugin)
        install(NotificationPartnershipPlugin)
        install(WebhookPartnershipPlugin)

        post {
            val videoId = call.parameters.getValue("videoId").toUUID()
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val userInfo = authRepository.getUserInfo(call.token)
            val video = repository.approve(eventSlug, partnershipId, videoId, userInfo)

            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            call.attributes.variables = NotificationVariables.SupportVideoApproved(
                language = partnership.language,
                event = eventRepository.getBySlug(eventSlug),
                company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId),
                partnership = partnership,
                videoUrl = video.url,
            )
            call.respond(HttpStatusCode.OK, video)
        }
    }
    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/support-videos/{videoId}/decline") {
        install(AuthorizedOrganisationPlugin)
        install(NotificationPartnershipPlugin)
        install(WebhookPartnershipPlugin)

        post {
            val videoId = call.parameters.getValue("videoId").toUUID()
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val request = call.receive<DeclineSupportVideoRequest>(schema = "decline_support_video.schema.json")
            val userInfo = authRepository.getUserInfo(call.token)
            val video = repository.decline(eventSlug, partnershipId, videoId, userInfo, request.reason)

            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            call.attributes.variables = NotificationVariables.SupportVideoDeclined(
                language = partnership.language,
                event = eventRepository.getBySlug(eventSlug),
                company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId),
                partnership = partnership,
                videoUrl = video.url,
                declineReason = request.reason,
            )
            call.respond(HttpStatusCode.OK, video)
        }
    }
}
