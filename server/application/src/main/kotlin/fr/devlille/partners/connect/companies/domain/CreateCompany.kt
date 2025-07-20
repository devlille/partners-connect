package fr.devlille.partners.connect.companies.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CreateCompany(
    val name: String,
    @SerialName("site_url")
    val siteUrl: String,
    val description: String? = null,
    val socials: List<Social> = emptyList(),
)
