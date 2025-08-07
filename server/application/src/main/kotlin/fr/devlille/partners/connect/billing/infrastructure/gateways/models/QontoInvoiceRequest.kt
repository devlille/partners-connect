package fr.devlille.partners.connect.billing.infrastructure.gateways.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoInvoiceRequest(
    val settings: QontoInvoiceSettings,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("due_date")
    val dueDate: String,
    @SerialName("issue_date")
    val issueDate: String,
    val currency: String,
    @SerialName("payment_methods")
    val paymentMethods: QontoPaymentMethods,
    @SerialName("purchase_order")
    val purchaseOrder: String?,
    val items: List<QontoInvoiceItem>,
)

@Serializable
data class QontoInvoiceSettings(
    @SerialName("legal_capital_share")
    val legalCapitalShare: QontoLegalCapitalShare,
)

@Serializable
data class QontoLegalCapitalShare(
    val currency: String,
)

@Serializable
data class QontoPaymentMethods(
    val iban: String,
)

@Serializable
data class QontoInvoiceItem(
    val title: String,
    val quantity: String,
    @SerialName("unit_price")
    val unitPrice: QontoMoneyAmount,
    @SerialName("vat_rate")
    val vatRate: String,
)

@Serializable
data class QontoMoneyAmount(
    val value: String,
    val currency: String,
)
