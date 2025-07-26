package fr.devlille.partners.connect.invoices.infrastructure.gateways

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoBillingAddress(
    @SerialName("street_address")
    val streetAddress: String,
    val city: String,
    @SerialName("zip_code")
    val zipCode: String,
    @SerialName("country_code")
    val countryCode: String,
)
