package fr.devlille.partners.connect.webhooks.domain

import java.util.UUID

interface WebhookRepository {
    /**
     * Send webhook notifications for a resource event to all configured webhook gateways
     */
    suspend fun sendWebhooks(
        eventSlug: String,
        resourceType: WebhookResourceType,
        resourceId: UUID,
        eventType: WebhookEventType,
    )
}
