package fr.devlille.partners.connect.webhooks.infrastructure.gateways

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.WebhookType
import fr.devlille.partners.connect.integrations.infrastructure.db.WebhookIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.webhooks.domain.EventWebhookData
import fr.devlille.partners.connect.webhooks.domain.PartnershipWebhookData
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
        // Get event entity and throw NotFoundException if it doesn't exist
        val eventEntity = transaction {
            EventEntity.findById(eventId) ?: throw NotFoundException("Event not found")
        }

        // Get partnership entity and throw NotFoundException if it doesn't exist
        val partnershipEntity = transaction {
            PartnershipEntity.findById(partnershipId) ?: throw NotFoundException("Partnership not found")
        }

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

        // Create webhook payload with actual entity data
        val payload = WebhookPayload(
            eventType = eventType,
            partnership = PartnershipWebhookData(
                id = partnershipId.toString(),
                companyId = partnershipEntity.company.id.value.toString(),
                packId = partnershipEntity.selectedPack?.id?.value?.toString(),
                status = when {
                    partnershipEntity.validatedAt != null -> "validated"
                    partnershipEntity.declinedAt != null -> "declined"
                    partnershipEntity.suggestionApprovedAt != null -> "suggestion_approved"
                    partnershipEntity.suggestionDeclinedAt != null -> "suggestion_declined"
                    else -> "pending"
                },
            ),
            event = EventWebhookData(
                id = eventId.toString(),
                slug = eventEntity.slug,
                name = eventEntity.name,
            ),
            timestamp = Clock.System.now().toString(),
        )

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
