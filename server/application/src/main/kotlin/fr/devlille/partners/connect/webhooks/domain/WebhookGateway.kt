package fr.devlille.partners.connect.webhooks.domain

import java.util.UUID

interface WebhookGateway {
    suspend fun sendWebhooks(eventId: UUID, payload: WebhookPayload): Boolean
}
