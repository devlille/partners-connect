package fr.devlille.partners.connect.billing.infrastructure.gateways.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoClientInvoiceResponse(
    @SerialName("client_invoice")
    val clientInvoice: ClientInvoice,
)

@Serializable
data class ClientInvoice(
    val id: String,
    val number: String,
    @SerialName("invoice_url") val invoiceUrl: String,
    @SerialName("created_at") val createdAt: String,
)
