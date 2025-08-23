package fr.devlille.partners.connect.webhooks.domain

import kotlinx.serialization.Serializable

@Serializable
data class WebhookPayload(
    val eventType: WebhookEventType,
    val partnership: PartnershipWebhookData,
    val event: EventWebhookData,
    val timestamp: String,
)

@Serializable
data class PartnershipWebhookData(
    val id: String,
    val companyId: String?,
    val packId: String?,
    val status: String,
)

@Serializable
data class EventWebhookData(
    val id: String,
    val slug: String,
    val name: String,
)
