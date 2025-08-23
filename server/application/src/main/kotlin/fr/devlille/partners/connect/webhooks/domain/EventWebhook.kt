package fr.devlille.partners.connect.webhooks.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class WebhookType {
    ALL,
    PARTNERSHIP,
}

enum class WebhookEventType {
    CREATED,
    UPDATED,
    DELETED,
}

@Serializable
data class EventWebhook(
    val id: String,
    val url: String,
    val type: WebhookType,
    @SerialName("partnership_id")
    val partnershipId: String?,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
    @SerialName("updated_at")
    val updatedAt: LocalDateTime,
)

@Serializable
data class CreateEventWebhookRequest(
    val url: String,
    val type: WebhookType,
    @SerialName("partnership_id")
    val partnershipId: String? = null,
    @SerialName("header_auth")
    val headerAuth: String? = null,
)
