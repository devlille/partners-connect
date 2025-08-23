package fr.devlille.partners.connect.webhooks.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class WebhookRepositoryExposed(
    private val webhookGateways: List<WebhookGateway>,
) : WebhookRepository {
    override suspend fun sendWebhooks(
        eventSlug: String,
        partnershipId: UUID,
        eventType: WebhookEventType,
    ) {
        // Separate transaction to get event entity
        val eventId = transaction {
            EventEntity.findBySlug(eventSlug)?.id?.value
                ?: throw NotFoundException("Event with slug $eventSlug not found")
        }

        // Separate transaction to get integration list
        val integrationRows = transaction {
            IntegrationsTable.findByEventIdAndUsage(eventId, IntegrationUsage.WEBHOOK)
        }

        for (row in integrationRows) {
            val provider = row[IntegrationsTable.provider]
            val integrationId = row[IntegrationsTable.id].value
            val gateway = webhookGateways.find { it.provider == provider }
                ?: throw NotFoundException("No gateway for provider $provider")

            gateway.sendWebhook(integrationId, eventId, partnershipId, eventType)
        }
    }
}
