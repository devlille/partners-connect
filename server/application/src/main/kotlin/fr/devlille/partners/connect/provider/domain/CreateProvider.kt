package fr.devlille.partners.connect.provider.domain

import kotlinx.serialization.Serializable

/**
 * Request data model for creating a new provider within an organisation.
 *
 * This data class represents the JSON payload expected by the POST /orgs/{orgSlug}/providers
 * endpoint. The organisation context is derived from the URL path parameter, not included
 * in the request body.
 *
 * @property name Display name for the provider (required, non-empty)
 * @property type Category or classification of the provider (required)
 * @property website Optional provider website URL
 * @property phone Optional contact phone number
 * @property email Optional contact email address
 */
@Serializable
data class CreateProvider(
    val name: String,
    val type: String,
    val website: String? = null,
    val phone: String? = null,
    val email: String? = null,
)
