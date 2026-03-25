package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SponsoringOptionWithCount(
    val option: SponsoringOptionWithTranslations,
    @SerialName("partnership_count") val partnershipCount: Int,
)
