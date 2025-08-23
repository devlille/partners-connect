package fr.devlille.partners.connect.webhooks.domain

import java.util.UUID

interface WebhookRepository {
    /**
     * Send webhook notifications for a partnership event to all configured webhook gateways
     */
    suspend fun sendWebhooks(
        eventSlug: String,
        partnershipId: UUID,
        eventType: WebhookEventType,
    )
}
