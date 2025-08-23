package fr.devlille.partners.connect.events.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class WebhookType {
    ALL,
    PARTNERSHIP,
}

@Serializable
data class EventWebhook(
    val id: String,
    val url: String,
    val type: WebhookType,
    val partnershipId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class CreateEventWebhookRequest(
    val url: String,
    val type: WebhookType,
    val partnershipId: String? = null,
    val headerAuth: String? = null,
)
