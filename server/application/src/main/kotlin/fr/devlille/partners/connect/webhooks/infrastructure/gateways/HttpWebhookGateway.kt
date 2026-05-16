package fr.devlille.partners.connect.webhooks.infrastructure.gateways

import fr.devlille.partners.connect.agenda.domain.Speaker
import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialEntity
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.WebhookType
import fr.devlille.partners.connect.integrations.infrastructure.db.WebhookIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.partnership.application.mappers.toDetailedDomain
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.BoothActivitiesTable
import fr.devlille.partners.connect.partnership.infrastructure.db.BoothActivityEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipSupportVideoEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.SpeakerPartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.SpeakerPartnershipTable
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.domain.WebhookPayload
import fr.devlille.partners.connect.webhooks.domain.WebhookResourceType
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class HttpWebhookGateway(
    private val httpClient: HttpClient,
) : WebhookGateway {
    override val provider = IntegrationProvider.WEBHOOK

    override suspend fun sendWebhook(
        integrationId: UUID,
        eventId: UUID,
        resourceType: WebhookResourceType,
        resourceId: UUID,
        eventType: WebhookEventType,
    ): Boolean {
        // Get integration configuration and check permissions in transaction
        val config = transaction {
            val webhookConfig = WebhookIntegrationsTable[integrationId]

            // Check if we can send webhook based on config type
            val canSend = when (webhookConfig.type) {
                WebhookType.ALL -> true
                WebhookType.PARTNERSHIP ->
                    resourceType == WebhookResourceType.PARTNERSHIP && webhookConfig.partnershipId == resourceId
            }

            if (!canSend) null else webhookConfig
        }

        // If we can't send webhook, return false
        if (config == null) return false

        val payload: WebhookPayload = when (resourceType) {
            WebhookResourceType.PARTNERSHIP -> buildPartnershipPayload(eventId, resourceId, eventType)
            WebhookResourceType.ECOSYSTEM_PARTNER -> buildEcosystemPartnerPayload(eventId, resourceId, eventType)
        }

        // Send HTTP call
        val response = httpClient.post(config.url) {
            contentType(ContentType.Application.Json)
            headers {
                // Add authentication header if provided
                config.headerAuth?.let { auth ->
                    append(HttpHeaders.Authorization, auth)
                }
            }
            setBody(Json.encodeToString(WebhookPayload.serializer(), payload))
        }

        return response.status.isSuccess()
    }

    @Suppress("LongMethod")
    private fun buildPartnershipPayload(
        eventId: UUID,
        resourceId: UUID,
        eventType: WebhookEventType,
    ): WebhookPayload.Partnership = transaction {
        val eventEntity = EventEntity.findById(eventId) ?: throw NotFoundException("Event not found")
        val billing = BillingEntity.singleByEventAndPartnership(eventEntity.id.value, resourceId)
        val partnership = PartnershipEntity.findById(resourceId)
            ?: throw NotFoundException("Partnership not found")
        val jobs = CompanyJobOfferPromotionEntity
            .listByPartnershipAndStatus(resourceId, status = PromotionStatus.APPROVED)
            .map { it.jobOffer.toDomain() }
        val activities = BoothActivityEntity
            .find { BoothActivitiesTable.partnershipId eq resourceId }
            .map { it.toDomain() }
        val questions = QandaQuestionEntity
            .find { QandaQuestionsTable.partnershipId eq resourceId }
            .map { it.toDomain() }
        val speakers = SpeakerPartnershipEntity
            .find { SpeakerPartnershipTable.partnershipId eq resourceId }
            .map { association ->
                val s = association.speaker
                Speaker(
                    id = s.id.value.toString(),
                    name = s.name,
                    biography = s.biography,
                    jobTitle = s.jobTitle,
                    photoUrl = s.photoUrl,
                    pronouns = s.pronouns,
                    company = s.company?.name,
                    externalId = s.externalId,
                    source = s.sourceProvider.name.lowercase(),
                )
            }
            .sortedBy { it.name }
        val supportVideoUrl = PartnershipSupportVideoEntity.singleByPartnership(resourceId)
            ?.takeIf { it.status == PromotionStatus.APPROVED }
            ?.url
        WebhookPayload.Partnership(
            eventType = eventType,
            resourceId = resourceId.toString(),
            partnership = partnership.toDetailedDomain(
                billing = billing,
                selectedPack = partnership.selectedPack
                    ?.toDomain(language = partnership.language, partnershipId = resourceId),
                suggestionPack = partnership.suggestionPack
                    ?.toDomain(language = partnership.language, partnershipId = resourceId),
                validatedPack = partnership.validatedPack()
                    ?.toDomain(language = partnership.language, partnershipId = resourceId),
            ),
            company = partnership.company
                .toDomain(partnership.company.socials.map(CompanySocialEntity::toDomain)),
            event = EventSummary(
                slug = eventEntity.slug,
                name = eventEntity.name,
                startTime = eventEntity.startTime,
                endTime = eventEntity.endTime,
                submissionStartTime = eventEntity.submissionStartTime,
                submissionEndTime = eventEntity.submissionEndTime,
            ),
            jobs = jobs,
            activities = activities,
            questions = questions,
            speakers = speakers,
            supportVideoUrl = supportVideoUrl,
            timestamp = Clock.System.now().toString(),
        )
    }

    private fun buildEcosystemPartnerPayload(
        eventId: UUID,
        resourceId: UUID,
        eventType: WebhookEventType,
    ): WebhookPayload.EcosystemPartner = transaction {
        val eventEntity = EventEntity.findById(eventId) ?: throw NotFoundException("Event not found")
        val partner = EcosystemPartnerEntity.findById(resourceId)
            ?: throw NotFoundException("Ecosystem partner not found")
        WebhookPayload.EcosystemPartner(
            eventType = eventType,
            resourceId = resourceId.toString(),
            company = partner.company.toDomain(partner.company.socials.map(CompanySocialEntity::toDomain)),
            event = EventSummary(
                slug = eventEntity.slug,
                name = eventEntity.name,
                startTime = eventEntity.startTime,
                endTime = eventEntity.endTime,
                submissionStartTime = eventEntity.submissionStartTime,
                submissionEndTime = eventEntity.submissionEndTime,
            ),
            timestamp = Clock.System.now().toString(),
            category = partner.category.name,
        )
    }
}
