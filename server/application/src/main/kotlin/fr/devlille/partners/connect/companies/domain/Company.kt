package fr.devlille.partners.connect.companies.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: String,
    val name: String,
    @SerialName("head_office")
    val headOffice: String,
    val siret: String,
    val description: String?,
    val siteUrl: String,
    val medias: Media?,
)

@Serializable
class Social(
    val type: SocialType,
    val url: String,
)
