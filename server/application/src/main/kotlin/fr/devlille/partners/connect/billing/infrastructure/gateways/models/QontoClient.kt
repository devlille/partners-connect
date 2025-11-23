package fr.devlille.partners.connect.billing.infrastructure.gateways.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoClient(
    val id: String,
    val type: String? = null,
    val name: String? = null,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    val email: String? = null,
    @SerialName("extra_emails")
    val extraEmails: List<String> = emptyList(),
    @SerialName("tax_identification_number")
    val taxId: String? = null,
    @SerialName("vat_number")
    val vatNumber: String? = null,
    val locale: String? = null,
    val address: String? = null,
    val city: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null,
    @SerialName("zip_code")
    val zipCode: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val currency: String? = null,
    @SerialName("billing_address")
    val billingAddress: QontoBillingAddress? = null,
)
