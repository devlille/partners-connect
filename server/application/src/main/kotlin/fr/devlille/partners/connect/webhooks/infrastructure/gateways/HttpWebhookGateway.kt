package fr.devlille.partners.connect.webhooks.infrastructure.gateways

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.integrations.domain.WebhookType
import fr.devlille.partners.connect.integrations.infrastructure.db.WebhookIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
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
import org.jetbrains.exposed.v1.jdbc.selectAll
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
            val eventData = EventsTable
                .selectAll()
                .where { EventsTable.id eq eventId }
                .singleOrNull()
                ?: return@transaction null

            val partnershipData = PartnershipsTable
                .selectAll()
                .where { PartnershipsTable.id eq partnershipId }
                .singleOrNull()
                ?: return@transaction null

            // Return data needed for webhook
            Triple(config, eventData, partnershipData)
        }

        // If we can't send webhook or data is missing, return false
        val (config, eventData, partnershipData) = webhookData ?: return false

        // Create webhook payload with actual entity data
        val payload = WebhookPayload(
            eventType = WebhookEventType.CREATED,
            partnership = PartnershipWebhookData(
                id = partnershipId.toString(),
                companyId = partnershipData[PartnershipsTable.companyId].value.toString(),
                packId = partnershipData[PartnershipsTable.selectedPackId]?.value?.toString(),
                status = when {
                    partnershipData[PartnershipsTable.validatedAt] != null -> "validated"
                    partnershipData[PartnershipsTable.declinedAt] != null -> "declined"
                    partnershipData[PartnershipsTable.suggestionApprovedAt] != null -> "approved"
                    partnershipData[PartnershipsTable.suggestionDeclinedAt] != null -> "suggestion_declined"
                    else -> "pending"
                },
            ),
            event = EventWebhookData(
                id = eventId.toString(),
                slug = eventData[EventsTable.slug],
                name = eventData[EventsTable.name],
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
