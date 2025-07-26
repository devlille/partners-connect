package fr.devlille.partners.connect.invoices.infrastructure.gateways

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoClient(
    val id: String,
    val type: String,
    val name: String,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    val email: String,
    @SerialName("extra_emails")
    val extraEmails: List<String> = emptyList(),
    @SerialName("tax_identification_number")
    val taxId: String? = null,
    @SerialName("vat_number")
    val vatNumber: String? = null,
    val locale: String,
    val address: String,
    val city: String,
    @SerialName("country_code")
    val countryCode: String,
    @SerialName("zip_code")
    val zipCode: String,
    @SerialName("created_at")
    val createdAt: String,
    val currency: String,
    @SerialName("billing_address")
    val billingAddress: QontoBillingAddress? = null,
)
