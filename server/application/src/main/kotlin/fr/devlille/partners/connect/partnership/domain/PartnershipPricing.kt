package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartnershipPricing(
    val basePrice: Int,
    val totalAmount: Int,
    @SerialName("required_options")
    val requiredOptions: List<OptionPricing>,
    @SerialName("optional_options")
    val optionalOptions: List<OptionPricing>,
)

@Serializable
data class OptionPricing(
    val label: String,
    val amount: Int,
    val selectedValue: String? = null,
    val required: Boolean = false,
)
