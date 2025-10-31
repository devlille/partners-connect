package fr.devlille.partners.connect.companies.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: String,
    val name: String,
    @SerialName("head_office")
    val headOffice: Address,
    val siret: String,
    val vat: String,
    val description: String?,
    @SerialName("site_url")
    val siteUrl: String,
    val medias: Media?,
    val status: CompanyStatus,
)

@Serializable
class Address(
    val address: String,
    val city: String,
    @SerialName("zip_code")
    val zipCode: String,
    val country: String,
)

@Serializable
class Social(
    val type: SocialType,
    val url: String,
)
