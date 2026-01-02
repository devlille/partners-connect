package fr.devlille.partners.connect.webhooks.infrastructure.gateways

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialEntity
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.WebhookType
import fr.devlille.partners.connect.integrations.infrastructure.db.WebhookIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.partnership.application.mappers.toDetailedDomain
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.domain.WebhookPayload
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
        partnershipId: UUID,
        eventType: WebhookEventType,
    ): Boolean {
        // Get integration configuration and check permissions in transaction
        val config = transaction {
            val webhookConfig = WebhookIntegrationsTable[integrationId]

            // Check if we can send webhook based on config type
            val canSend = when (webhookConfig.type) {
                WebhookType.ALL -> true
                WebhookType.PARTNERSHIP -> webhookConfig.partnershipId == partnershipId
            }

            if (!canSend) null else webhookConfig
        }

        // If we can't send webhook, return false
        if (config == null) return false

        // Get partnership entity and throw NotFoundException if it doesn't exist
        val payload = transaction {
            val eventEntity = EventEntity.findById(eventId) ?: throw NotFoundException("Event not found")
            val billing = BillingEntity
                .singleByEventAndPartnership(eventEntity.id.value, partnershipId)
            val partnership = PartnershipEntity.findById(partnershipId)
                ?: throw NotFoundException("Partnership not found")
            WebhookPayload(
                eventType = eventType,
                partnership = partnership.toDetailedDomain(
                    billing = billing,
                    selectedPack = partnership.selectedPack?.toDomain(
                        language = partnership.language,
                        partnershipId = partnershipId,
                    ),
                    suggestionPack = partnership.suggestionPack?.toDomain(
                        language = partnership.language,
                        partnershipId = partnershipId,
                    ),
                    validatedPack = partnership.validatedPack()?.toDomain(
                        language = partnership.language,
                        partnershipId = partnershipId,
                    ),
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
                timestamp = Clock.System.now().toString(),
            )
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
}
