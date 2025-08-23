package fr.devlille.partners.connect.webhooks.domain

import java.util.UUID

interface WebhookRepository {
    /**
     * Send webhook notifications for a partnership event to all configured webhook gateways
     */
    suspend fun sendWebhooks(
        eventId: UUID,
        partnershipId: UUID,
        eventType: WebhookEventType,
    ): Int
}
