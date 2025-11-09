package fr.devlille.partners.connect.provider.domain

import kotlinx.serialization.Serializable

/**
 * Request data model for updating an existing provider within an organisation.
 *
 * This data class represents the JSON payload expected by the PUT /orgs/{orgSlug}/providers/{id}
 * endpoint. All fields are optional to support partial updates. Only provided fields will be
 * updated, null values are ignored.
 *
 * @property name Optional new display name for the provider
 * @property type Optional new category or classification
 * @property website Optional new website URL (null to clear existing value)
 * @property phone Optional new contact phone number (null to clear existing value)
 * @property email Optional new contact email address (null to clear existing value)
 */
@Serializable
data class UpdateProvider(
    val name: String? = null,
    val type: String? = null,
    val website: String? = null,
    val phone: String? = null,
    val email: String? = null,
)
