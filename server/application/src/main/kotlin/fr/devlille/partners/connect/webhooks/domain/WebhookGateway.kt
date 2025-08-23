package fr.devlille.partners.connect.webhooks.domain

import fr.devlille.partners.connect.integrations.domain.WebhookType
import java.util.UUID

enum class WebhookEventType {
    CREATED,
    UPDATED,
    DELETED,
}

interface WebhookGateway {
    /**
     * Send webhook notification by merging configuration retrieval, permission check, and HTTP call
     */
    suspend fun sendWebhook(
        integrationId: UUID,
        eventId: UUID,
        partnershipId: UUID,
    ): Boolean
}

data class WebhookConfig(
    val url: String,
    val headerAuth: String?,
    val type: WebhookType,
    val partnershipId: UUID?,
)
