package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.organisations.infrastructure.api.orgSlug
import fr.devlille.partners.connect.partnership.domain.DeclineJobOfferRequest
import fr.devlille.partners.connect.partnership.domain.PartnershipJobOfferRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

/**
 * Routes for partnership job offer promotions.
 * Separates public endpoints (listPartnershipJobOffers) from protected endpoints.
 */
fun Route.publicPartnershipJobOfferRoutes() {
    val repository by inject<PartnershipJobOfferRepository>()

    route("/events/{eventSlug}/partnerships/{partnershipId}/job-offers") {
        get {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val statusParam = call.request.queryParameters["status"]
            val status = statusParam?.let { PromotionStatus.valueOf(it.uppercase()) }
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE

            val promotions = repository.listPartnershipJobOffers(
                eventSlug = eventSlug,
                partnershipId = partnershipId,
                status = status,
                page = page,
                pageSize = pageSize,
            )
            call.respond(HttpStatusCode.OK, promotions)
        }
    }
}

@Suppress("LongMethod")
fun Route.orgsPartnershipJobOfferRoutes() {
    val repository by inject<PartnershipJobOfferRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/job-offers") {
        install(AuthorizedOrganisationPlugin)

        get {
            val orgSlug = call.parameters.orgSlug
            val eventSlug = call.parameters.eventSlug
            val statusParam = call.request.queryParameters["status"]
            val status = statusParam?.let { PromotionStatus.valueOf(it.uppercase()) }
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE

            val promotions = repository.listEventJobOffers(
                orgSlug = orgSlug,
                eventSlug = eventSlug,
                status = status,
                page = page,
                pageSize = pageSize,
            )
            call.respond(HttpStatusCode.OK, promotions)
        }
    }
}

fun Route.orgsPartnershipJobOfferDecisionRoutes() {
    val repository by inject<PartnershipJobOfferRepository>()
    val authRepository by inject<AuthRepository>()
    val notificationRepository by inject<NotificationRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val eventRepository by inject<EventRepository>()
    val webhookRepository by inject<WebhookRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers/{promotionId}") {
        install(AuthorizedOrganisationPlugin)
        post("/approve") {
            val promotionId = call.parameters.getValue("promotionId").toUUID()
            val eventSlug = call.parameters.eventSlug
            val userInfo = authRepository.getUserInfo(call.token)
            val promotion = repository.approvePromotion(promotionId = promotionId, reviewer = userInfo)

            // Send notification after successful approval
            val partnershipId = promotion.partnershipId.toUUID()
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val event = eventRepository.getBySlug(eventSlug)
            val variables = NotificationVariables.JobOfferApproved(
                language = partnership.language,
                event = event,
                company = company,
                partnership = partnership,
                jobOffer = promotion.jobOffer,
            )
            notificationRepository.sendMessage(eventSlug, variables)

            // Send webhook notification for job offer approval
            webhookRepository.sendWebhooks(eventSlug, partnershipId, WebhookEventType.UPDATED)

            call.respond(HttpStatusCode.OK, promotion)
        }

        post("/decline") {
            val promotionId = call.parameters.getValue("promotionId").toUUID()
            val eventSlug = call.parameters.eventSlug
            val request = call.receive<DeclineJobOfferRequest>(schema = "decline_job_offer_promotion.schema.json")
            val userInfo = authRepository.getUserInfo(call.token)
            val promotion = repository
                .declinePromotion(promotionId = promotionId, reviewer = userInfo, reason = request.reason)

            // Send notification after successful decline
            val partnershipId = promotion.partnershipId.toUUID()
            val company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId)
            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            val event = eventRepository.getBySlug(eventSlug)
            val variables = NotificationVariables.JobOfferDeclined(
                language = partnership.language,
                event = event,
                company = company,
                partnership = partnership,
                jobOffer = promotion.jobOffer,
                declineReason = request.reason,
            )
            notificationRepository.sendMessage(eventSlug, variables)

            // Send webhook notification for job offer decline
            webhookRepository.sendWebhooks(eventSlug, partnershipId, WebhookEventType.UPDATED)

            call.respond(HttpStatusCode.OK, promotion)
        }
    }
}
