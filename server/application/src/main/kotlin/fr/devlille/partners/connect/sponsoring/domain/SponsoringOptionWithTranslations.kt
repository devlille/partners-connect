package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.Serializable

@Serializable
data class SponsoringOptionWithTranslations(
    val id: String,
    val translations: Map<String, OptionTranslation>,
    val price: Int?,
)
