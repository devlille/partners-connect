package fr.devlille.partners.connect.webhooks.domain

import java.util.UUID

enum class WebhookEventType {
    CREATED,
    UPDATED,
    DELETED,
}

interface WebhookGateway {
    /**
     * Get the integration configuration from an integration id
     */
    fun getIntegrationConfiguration(integrationId: UUID): WebhookConfig?

    /**
     * From the event id, be sure that we can send a webhook to the webhook consumer
     */
    suspend fun canSendWebhook(eventId: UUID, integrationId: UUID): Boolean

    /**
     * Send a http call with the configuration in the integration table to send the data to external service
     */
    suspend fun sendHttpCall(integrationId: UUID, payload: WebhookPayload): Boolean
}

data class WebhookConfig(
    val url: String,
    val headerAuth: String?,
    val type: String,
    val partnershipId: UUID?,
)
