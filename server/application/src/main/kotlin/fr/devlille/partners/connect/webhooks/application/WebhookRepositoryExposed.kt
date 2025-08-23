package fr.devlille.partners.connect.webhooks.application

import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class WebhookRepositoryExposed(
    private val webhookGateways: List<WebhookGateway>,
) : WebhookRepository {
    override suspend fun sendWebhooks(
        eventId: UUID,
        partnershipId: UUID,
        eventType: WebhookEventType,
    ): Int {
        // Get all webhook integrations for this event
        val integrationIds = transaction {
            IntegrationsTable
                .findByEventIdAndUsage(eventId, IntegrationUsage.WEBHOOK)
                .map { it[IntegrationsTable.id].value }
        }

        return integrationIds.sumOf { integrationId ->
            sendWebhookForIntegration(integrationId, eventId, partnershipId, eventType)
        }
    }

    private suspend fun sendWebhookForIntegration(
        integrationId: UUID,
        eventId: UUID,
        partnershipId: UUID,
        eventType: WebhookEventType,
    ): Int {
        return webhookGateways.count { gateway ->
            try {
                gateway.sendWebhook(integrationId, eventId, partnershipId, eventType)
            } catch (_: IllegalArgumentException) {
                // Invalid webhook configuration
                false
            } catch (_: java.net.ConnectException) {
                // Network connection failed
                false
            } catch (_: java.io.IOException) {
                // IO error during HTTP call
                false
            }
        }
    }
}
