package fr.devlille.partners.connect.provider.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Provider domain model representing a service or product provider within an organisation.
 *
 * Providers are organisation-scoped entities that can be attached to events for partnership
 * management. This data class represents the public API response structure with organisation
 * context included for client consumption.
 *
 * @property id Unique identifier for the provider (UUID format)
 * @property name Display name of the provider
 * @property type Category or type of provider (e.g., "service", "product", "sponsor")
 * @property website Optional website URL for the provider
 * @property phone Optional contact phone number
 * @property email Optional contact email address
 * @property orgSlug Organisation slug identifier for scoping context
 * @property createdAt Timestamp when the provider was created
 */
@Serializable
data class Provider(
    val id: String,
    val name: String,
    val type: String,
    val website: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("org_slug")
    val orgSlug: String,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)
