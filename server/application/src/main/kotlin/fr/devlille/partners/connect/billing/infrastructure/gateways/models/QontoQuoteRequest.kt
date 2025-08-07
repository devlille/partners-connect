package fr.devlille.partners.connect.billing.infrastructure.gateways.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoQuoteRequest(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("issue_date")
    val issueDate: String,
    @SerialName("expiry_date")
    val expiryDate: String,
    val currency: String,
    val items: List<QontoInvoiceItem>,
    @SerialName("terms_and_conditions")
    val termsAndConditions: String,
)
