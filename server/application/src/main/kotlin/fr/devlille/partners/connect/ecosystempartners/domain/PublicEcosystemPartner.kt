package fr.devlille.partners.connect.ecosystempartners.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicEcosystemPartnerGroup(
    val category: String,
    val partners: List<PublicEcosystemPartner>,
)

@Serializable
data class PublicEcosystemPartner(
    val id: String,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @SerialName("site_url")
    val siteUrl: String? = null,
)
