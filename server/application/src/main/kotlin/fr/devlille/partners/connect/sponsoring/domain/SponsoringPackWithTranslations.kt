package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SponsoringPackWithTranslations(
    val id: String,
    val name: String,
    @SerialName("base_price")
    val basePrice: Int,
    @SerialName("max_quantity")
    val maxQuantity: Int?,
    @SerialName("required_options")
    val requiredOptions: List<SponsoringOptionWithTranslations>,
    @SerialName("optional_options")
    val optionalOptions: List<SponsoringOptionWithTranslations>,
)
