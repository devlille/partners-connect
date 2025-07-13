package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SponsoringPack(
    val id: String,
    val name: String,
    @SerialName("base_price")
    val basePrice: Int,
    @SerialName("max_quantity")
    val maxQuantity: Int?,
    @SerialName("required_options")
    val requiredOptions: List<SponsoringOption>,
    @SerialName("optional_options")
    val optionalOptions: List<SponsoringOption>,
)

@Serializable
data class SponsoringOption(
    val id: String,
    val name: String,
    val description: String?,
    val price: Int?,
)
