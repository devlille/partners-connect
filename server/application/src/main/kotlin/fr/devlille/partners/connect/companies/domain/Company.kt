package fr.devlille.partners.connect.companies.domain

import kotlinx.serialization.Serializable

@Serializable
class Company(
    val id: String,
    val name: String,
    val description: String?,
    val siteUrl: String,
    val medias: Media?,
)

@Serializable
class Social(
    val type: SocialType,
    val url: String,
)
