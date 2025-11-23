package fr.devlille.partners.connect.billing.infrastructure.gateways.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoBillingAddress(
    @SerialName("street_address")
    val streetAddress: String? = null,
    val city: String? = null,
    @SerialName("zip_code")
    val zipCode: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null,
)
