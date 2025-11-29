package fr.devlille.partners.connect.companies.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateCompany(
    val name: String,
    @SerialName("site_url")
    val siteUrl: String? = null,
    @SerialName("head_office")
    val headOffice: Address? = null,
    val siret: String? = null,
    val vat: String? = null,
    val description: String? = null,
    val socials: List<Social> = emptyList(),
)
