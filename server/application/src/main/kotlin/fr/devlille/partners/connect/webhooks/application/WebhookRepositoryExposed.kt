package fr.devlille.partners.connect.webhooks.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
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
        val eventId = transaction {
            EventEntity.findBySlug(eventSlug)?.id?.value
                ?: throw NotFoundException("Event with slug $eventSlug not found")
        }
        val integrations = transaction {
            IntegrationEntity
                .find { IntegrationsTable.eventId eq eventId and (IntegrationsTable.usage eq IntegrationUsage.WEBHOOK) }
                .toList()
        }
        for (integration in integrations) {
            val gateway = webhookGateways.find { it.provider == integration.provider }
                ?: throw NotFoundException("No gateway for provider ${integration.provider}")
            gateway.sendWebhook(integration.id.value, eventId, partnershipId, eventType)
        }
    }
}
