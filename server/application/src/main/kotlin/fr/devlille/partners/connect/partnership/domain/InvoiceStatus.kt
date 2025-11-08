package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InvoiceStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("sent")
    SENT,

    @SerialName("paid")
    PAID,
}
