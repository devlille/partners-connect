package fr.devlille.partners.connect.organisations.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrganisationListResponse(
    val name: String,
    val slug: String,
    @SerialName("head_office")
    val headOffice: String,
    val owner: Owner,
)

@Serializable
data class Owner(
    @SerialName("display_name")
    val displayName: String,
    val email: String,
)
