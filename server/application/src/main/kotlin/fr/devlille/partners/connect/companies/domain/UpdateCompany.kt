package fr.devlille.partners.connect.companies.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Partial update model for company information.
 *
 * All fields are optional - null values indicate "no change",
 * non-null values replace existing data.
 * Validation rules from CreateCompany apply to non-null fields.
 */
@Serializable
data class UpdateCompany(
    val name: String? = null,
    @SerialName("site_url")
    val siteUrl: String? = null,
    @SerialName("head_office")
    val headOffice: Address? = null,
    val siret: String? = null,
    val vat: String? = null,
    val description: String? = null,
    val socials: List<Social>? = null,
)
