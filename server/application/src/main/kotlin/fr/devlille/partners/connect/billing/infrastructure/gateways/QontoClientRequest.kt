package fr.devlille.partners.connect.billing.infrastructure.gateways

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoClientRequest(
    val name: String,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    val type: String,
    val email: String,
    @SerialName("extra_emails")
    val extraEmails: List<String> = emptyList(),
    @SerialName("vat_number")
    val vatNumber: String? = null,
    @SerialName("tax_identification_number")
    val taxId: String? = null,
    @SerialName("billing_address")
    val billingAddress: QontoBillingAddress,
    val currency: String,
    val locale: String,
)
