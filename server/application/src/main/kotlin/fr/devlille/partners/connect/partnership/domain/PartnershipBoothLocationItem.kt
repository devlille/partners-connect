package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartnershipBoothLocationItem(
    val partnership: PartnershipItem,
    @SerialName("booth_location")
    val boothLocation: String? = null,
)
