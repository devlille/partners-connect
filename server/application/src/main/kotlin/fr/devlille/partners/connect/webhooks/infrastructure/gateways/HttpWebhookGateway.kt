package fr.devlille.partners.connect.webhooks.infrastructure.gateways

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
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
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class HttpWebhookGateway(
    private val httpClient: HttpClient,
) : WebhookGateway {
    override suspend fun sendWebhook(
        integrationId: UUID,
        eventId: UUID,
        partnershipId: UUID,
    ): Boolean {
        // Get integration configuration and check permissions in transaction
        val webhookData = transaction {
            val config = WebhookIntegrationsTable[integrationId]

            // Check if we can send webhook based on config type
            val canSend = when (config.type) {
                WebhookType.ALL -> true
                WebhookType.PARTNERSHIP -> config.partnershipId == partnershipId
            }

            if (!canSend) return@transaction null

            // Get event entity and partnership entity from identifiers
            val eventEntity = EventEntity.findById(eventId)
                ?: return@transaction null

            val partnershipEntity = PartnershipEntity.findById(partnershipId)
                ?: return@transaction null

            // Return data needed for webhook
            Triple(config, eventEntity, partnershipEntity)
        }

        // If we can't send webhook or data is missing, return false
        val (config, eventEntity, partnershipEntity) = webhookData ?: return false

        // Create webhook payload with actual entity data
        val payload = WebhookPayload(
            eventType = WebhookEventType.CREATED,
            partnership = PartnershipWebhookData(
                id = partnershipId.toString(),
                companyId = partnershipEntity.company.id.value.toString(),
                packId = partnershipEntity.selectedPack?.id?.value?.toString(),
                status = when {
                    partnershipEntity.validatedAt != null -> "validated"
                    partnershipEntity.declinedAt != null -> "declined"
                    partnershipEntity.suggestionApprovedAt != null -> "approved"
                    partnershipEntity.suggestionDeclinedAt != null -> "suggestion_declined"
                    else -> "pending"
                },
            ),
            event = EventWebhookData(
                id = eventId.toString(),
                slug = eventEntity.slug,
                name = eventEntity.name,
            ),
            timestamp = kotlinx.datetime.Clock.System.now().toString(),
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
