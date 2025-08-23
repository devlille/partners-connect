package fr.devlille.partners.connect.webhooks.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

enum class WebhookEventType {
    CREATED,
    UPDATED,
    DELETED,
}

interface WebhookGateway {
    val provider: IntegrationProvider

    /**
     * Send webhook notification by merging configuration retrieval, permission check, and HTTP call
     */
    suspend fun sendWebhook(
        integrationId: UUID,
        eventId: UUID,
        partnershipId: UUID,
        eventType: WebhookEventType,
    ): Boolean
}
